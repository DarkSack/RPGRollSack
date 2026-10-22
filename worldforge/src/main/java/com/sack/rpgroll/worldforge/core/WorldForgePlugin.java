package com.sack.rpgroll.worldforge.core;

import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.command.WorldForgeCommand;
import com.sack.rpgroll.worldforge.diagnostics.Diagnostics;
import com.sack.rpgroll.worldforge.dimension.Dimensions;
import com.sack.rpgroll.worldforge.generation.GenerationProvider;
import com.sack.rpgroll.worldforge.generation.Providers;
import com.sack.rpgroll.worldforge.gravity.Gravity;
import com.sack.rpgroll.worldforge.gui.Gui;
import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.profile.ProfileService;
import com.sack.rpgroll.worldforge.profile.ProfileValidator;
import com.sack.rpgroll.worldforge.profile.ServerCatalog;
import com.sack.rpgroll.worldforge.protection.Protections;
import com.sack.rpgroll.worldforge.regen.Backups;
import com.sack.rpgroll.worldforge.regen.Jobs;
import com.sack.rpgroll.worldforge.regen.Regeneration;
import com.sack.rpgroll.worldforge.storage.Database;
import com.sack.rpgroll.worldforge.storage.Records;
import com.sack.rpgroll.worldforge.template.Templates;
import com.sack.rpgroll.worldforge.terrain.Terrains;
import com.sack.rpgroll.worldforge.worlds.WorldService;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wires the services by hand (no container): each one gets exactly what it needs.
 * If the database cannot open, WorldForge stays up in read-only diagnostic mode instead
 * of guessing.
 */
public final class WorldForgePlugin extends JavaPlugin implements Listener {

    private ServerPlatform platform;
    private Messages messages;
    private Providers providers;
    private ServerCatalog catalog;
    private ProfileService profiles;
    private Database db;
    private String dbError;
    private Records records;
    private WorldService worlds;
    private Diagnostics diagnostics;
    private Protections protections;
    private Jobs jobs;
    private Regeneration regeneration;
    private Gui gui;
    private Dimensions dimensions;
    private Templates templates;
    private Terrains terrains;
    private final List<String> configProblems = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG, LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        if (!Files.exists(dataPath().resolve("messages.yml"))) {
            saveResource("messages.yml", false);
        }

        platform = new ServerPlatform(this);
        messages = Messages.fromJar(WorldForgePlugin.class);
        templates = new Templates(dataPath().resolve("structures"));
        terrains = new Terrains(dataPath().resolve("terrains"));
        providers = new Providers(name -> {
            Result<Structure> r = templates.load(name);
            if (!r.isOk()) {
                getLogger().warning("✘ Plantilla " + name + ": " + r.errors().getFirst().message() + " — esa regla no se aplica.");
            }
            return r;
        }, terrains::get);
        catalog = new ServerCatalog(templates, terrains);
        profiles = new ProfileService(dataPath(), getConfig().getLong("limits.max-import-bytes", 262_144),
                this::validator);
        protections = new Protections(dataPath().resolve("protections.yml"));
        reloadEverything();

        try {
            db = Database.open(dataPath().resolve("worldforge.db"));
            records = new Records(db);
            worlds = new WorldService(platform, profiles, providers, records, getLogger());
            jobs = new Jobs(db);
            dimensions = new Dimensions(platform, db, records,
                    () -> getServer().getWorldContainer().toPath().resolve(getServer().getWorlds().getFirst().getName()),
                    terrains, WorldForgePlugin::vanillaData);
            Path container = getServer().getWorldContainer().toPath();
            regeneration = new Regeneration(platform, jobs, records, new Backups(dataPath().resolve("backups"), container),
                    protections, worlds, container, getLogger(),
                    () -> getConfig().getInt("regeneration.max-chunks", 50_000),
                    () -> getConfig().getInt("regeneration.confirm-minutes", 15));
            // STARTUP: this runs before the server opens the worlds, so their files are free to edit.
            for (String line : regeneration.startup()) {
                getLogger().warning(line);
            }
        } catch (Exception e) {
            dbError = e.getMessage();
            getLogger().severe("✘ Base de datos no disponible: " + dbError + " — WorldForge queda en modo diagnóstico.");
        }
        diagnostics = new Diagnostics(getPluginMeta().getVersion(), platform, dataPath(), profiles, providers, db,
                dbError, records, worlds, this::configProblems, protections, jobs, dimensions);

        WorldForgeCommand command = new WorldForgeCommand(this);
        PluginCommand pc = getCommand("worldforge");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(this, this);
        gui = new Gui(this);
        getServer().getPluginManager().registerEvents(gui, this);
        if (worlds != null) {
            getServer().getPluginManager().registerEvents(new Gravity(platform, worlds), this);
        }

        long usable = profiles.all().stream().filter(ProfileService.Entry::usable).count();
        getLogger().info("✔ WorldForge habilitado en " + platform.name() + ": " + usable + " presets utilizables"
                + (db == null ? ", SIN base de datos" : "") + ".");
    }

    /** The server finished starting (all worlds loaded): re-check profiles and reopen managed worlds. */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (catalog.deferred()) {
            catalog.clearDeferred();
            platform.async(() -> {
                profiles.revalidate();
                return null;
            });
        }
        if (worlds != null) {
            worlds.restoreAtStartup();
            dimensions.reconcile().thenAccept(lines -> lines.forEach(getLogger()::info));
        }
    }

    @Override
    public void onDisable() {
        if (platform != null) {
            platform.cancelAll();
        }
        if (db != null) {
            db.close();
        }
    }

    /**
     * {@code bukkit.yml → worlds: <name>: generator: RPGRoll-WorldForge:<preset>}. Called while
     * the server loads its worlds, which is why the plugin is {@code load: STARTUP}.
     * An unusable preset throws: the world must not silently fall back to vanilla.
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (profiles == null || id == null || id.isBlank()) {
            throw new IllegalStateException("WorldForge: falta el id del preset en bukkit.yml (generator: RPGRoll-WorldForge:<preset>) para " + worldName);
        }
        Optional<ProfileService.Entry> e = profiles.preset(id);
        if (e.isEmpty() || !e.get().usable()) {
            throw new IllegalStateException("WorldForge: el preset «" + id + "» de bukkit.yml para " + worldName
                    + " no existe o tiene errores: " + e.map(ProfileService.Entry::problems).orElse(List.of()));
        }
        GenerationProvider provider = providers.get(e.get().profile().provider()).orElseThrow();
        ChunkGenerator g = provider.generator(worldName, e.get().profile());
        if (g == null) {
            getLogger().info(worldName + ": el preset «" + id + "» usa el generador de Minecraft; bukkit.yml no cambia nada.");
        }
        return g;
    }

    /** Re-reads config, texts and presets. Returns what could not be read. Off the tick thread. */
    public synchronized List<String> reloadEverything() {
        configProblems.clear();
        reloadConfig();
        configProblems.addAll(messages.load(dataPath().resolve("messages.yml")));
        // Before the presets: a preset with provider terrain is checked against them.
        try {
            configProblems.addAll(terrains.load(id -> resourceText("terrains/" + id + ".yml")));
        } catch (IOException e) {
            configProblems.add("no se pudieron leer los terrenos: " + e.getMessage());
        }
        try {
            profiles.load(this::builtInPreset);
        } catch (IOException e) {
            configProblems.add("no se pudieron leer los presets: " + e.getMessage());
        }
        profiles.loadProblems().forEach(p -> configProblems.add(p.toString()));
        protections.load().forEach(p -> configProblems.add("protections.yml" + (p.field().isEmpty() ? "" : " → " + p.field()) + ": " + p.message()));
        return List.copyOf(configProblems);
    }

    private synchronized List<String> configProblems() {
        return List.copyOf(configProblems);
    }

    private String builtInPreset(String id) {
        return resourceText("presets/" + id + ".yml");
    }

    private String resourceText(String path) {
        try (InputStream in = getResource(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** Vanilla's own worldgen JSON, {@code data/minecraft/<path>.json}, from the server jar. Null if absent. */
    private static String vanillaData(String path) {
        try (InputStream in = org.bukkit.Bukkit.getServer().getClass().getClassLoader()
                .getResourceAsStream("data/minecraft/" + path + ".json")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public List<String> importFiles() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dataPath().resolve("imports"), "*.yml")) {
            files.forEach(f -> out.add(f.getFileName().toString()));
        } catch (IOException ignored) {
            // no folder yet
        }
        return out;
    }

    public ProfileValidator validator() {
        return new ProfileValidator(providers.capabilities(), catalog);
    }

    public Path dataPath() {
        return getDataFolder().toPath();
    }

    public boolean ready() {
        return db != null;
    }

    public String notReadyReason() {
        return dbError == null ? "arrancando" : dbError;
    }

    public ServerPlatform platform() {
        return platform;
    }

    public Messages messages() {
        return messages;
    }

    public Providers providers() {
        return providers;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public Records records() {
        return records;
    }

    public WorldService worlds() {
        return worlds;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    public Templates templates() {
        return templates;
    }

    public Protections protections() {
        return protections;
    }

    public Regeneration regeneration() {
        return regeneration;
    }

    public Gui gui() {
        return gui;
    }

    public Dimensions dimensions() {
        return dimensions;
    }

    public Terrains terrains() {
        return terrains;
    }
}
