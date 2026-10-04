package com.sack.rpgroll.machines;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.machines.command.MachinesCommand;
import com.sack.rpgroll.machines.core.Displays;
import com.sack.rpgroll.machines.furnace.FurnaceListener;
import com.sack.rpgroll.machines.furnace.FurnaceMenu;
import com.sack.rpgroll.machines.furnace.FurnaceService;
import com.sack.rpgroll.machines.furnace.FurnaceSettings;
import com.sack.rpgroll.machines.quarry.Claims;
import com.sack.rpgroll.machines.quarry.QuarryListener;
import com.sack.rpgroll.machines.quarry.QuarryMenu;
import com.sack.rpgroll.machines.quarry.QuarryService;
import com.sack.rpgroll.machines.quarry.QuarrySettings;
import com.sack.rpgroll.machines.quarry.QuarryStore;
import com.sack.rpgroll.machines.quarry.QuarryWorker;
import com.sack.rpgroll.machines.spawner.SpawnerListener;
import com.sack.rpgroll.machines.spawner.SpawnerMenu;
import com.sack.rpgroll.machines.spawner.SpawnerService;
import com.sack.rpgroll.machines.spawner.SpawnerSettings;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

/**
 * RPGRoll-Machines: hornos, altos hornos y ahumadores mejorables (furnaces.yml), spawners con
 * mejoras, pila y recogida con toque de seda (spawners.yml) y canteras que excavan solas
 * (quarries.yml).
 */
public class MachinesPlugin extends JavaPlugin {

    private static final List<String> FILES = List.of("furnaces.yml", "spawners.yml", "quarries.yml");

    private LangManager lang;
    private FurnaceService furnaces;
    private SpawnerService spawners;
    private QuarryService quarries;
    private QuarryStore store;
    private QuarryWorker worker;
    private BukkitTask workerTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        for (String file : FILES) {
            if (!new File(getDataFolder(), file).exists()) {
                saveResource(file, false);
            }
        }
        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        lang.reload(getConfig().getString("language", "es"));

        Displays displays = new Displays(this);
        furnaces = new FurnaceService(this, lang, displays, FurnaceSettings.from(load("furnaces.yml"), this::warn));
        spawners = new SpawnerService(this, lang, displays, SpawnerSettings.from(load("spawners.yml"), this::warn));
        store = new QuarryStore(this);
        store.load();
        quarries = new QuarryService(this, lang, displays, store, new Claims(getLogger()),
                QuarrySettings.from(load("quarries.yml"), this::warn));
        worker = new QuarryWorker(quarries);

        var events = getServer().getPluginManager();
        events.registerEvents(new FurnaceListener(this, furnaces,
                (player, block) -> new FurnaceMenu(player, block, furnaces, lang).open()), this);
        events.registerEvents(new SpawnerListener(spawners, lang, displays, (player, block) -> spawners.spawner(block)
                .ifPresent(s -> new SpawnerMenu(player, block, spawners, lang, s.getSpawnedType()).open())), this);
        events.registerEvents(new QuarryListener(quarries, lang,
                (player, quarry) -> new QuarryMenu(player, quarry, quarries, lang).open()), this);
        events.registerEvents(worker, this);

        BrigadierCommands.register(this, "machines", "Máquinas de RPGRoll: dar, ver y recargar",
                List.of("maquinas", "rpgmachines"),
                new MachinesCommand(this, lang, furnaces, spawners, quarries), null, MachinesCommand.PERMISSION);

        // La receta y las de horno (para el autofundido), cuando todos los plugins ya registraron las suyas.
        getServer().getScheduler().runTask(this, () -> {
            quarries.registerRecipe();
            worker.reloadRecipes();
        });
        workerTask = getServer().getScheduler().runTaskTimer(this, worker, 20L, 1L);
        saveTask = getServer().getScheduler().runTaskTimer(this, () -> store.save(false), 200L, 200L);
        checkClaims();
    }

    @Override
    public void onDisable() {
        if (workerTask != null) {
            workerTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (store != null) {
            store.save(true);
        }
        if (quarries != null) {
            quarries.unregisterRecipe();
        }
    }

    /** /machines reload: config, idioma y los tres ficheros. Las canteras siguen donde iban. */
    public void reload() {
        reloadConfig();
        lang.reload(getConfig().getString("language", "es"));
        furnaces.settings(FurnaceSettings.from(load("furnaces.yml"), this::warn));
        spawners.settings(SpawnerSettings.from(load("spawners.yml"), this::warn));
        quarries.settings(QuarrySettings.from(load("quarries.yml"), this::warn));
        quarries.registerRecipe();
        worker.reloadRecipes();
        checkClaims();
    }

    private void checkClaims() {
        if (quarries.settings().enabled() && quarries.settings().requireClaim() && !quarries.claims().available()) {
            getLogger().warning("• quarries.yml pide claims (require-claim) pero GriefPrevention no está: "
                    + "las canteras se podrán poner en cualquier sitio.");
        }
    }

    /**
     * Un fichero de la carpeta del plugin, sin el del jar como respaldo: si no, los niveles de
     * serie se mezclarían con los propios (las secciones del respaldo suman claves).
     */
    private FileConfiguration load(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

    private void warn(String message) {
        getLogger().warning("✘ " + message);
    }
}
