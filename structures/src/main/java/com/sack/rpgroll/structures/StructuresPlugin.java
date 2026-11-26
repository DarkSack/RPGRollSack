package com.sack.rpgroll.structures;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.gui.ChatPromptManager;
import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.ConstructionEngine;
import com.sack.rpgroll.common.structure.BlockSources;
import com.sack.rpgroll.structures.catalog.StructureLoader;
import com.sack.rpgroll.structures.catalog.StructureRegistry;
import com.sack.rpgroll.structures.command.StructuresAdminCommand;
import com.sack.rpgroll.structures.command.StructuresCommand;
import com.sack.rpgroll.structures.config.CategorySettings;
import com.sack.rpgroll.structures.config.PerformanceSettings;
import com.sack.rpgroll.structures.config.StructuresSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.deposit.DepositService;
import com.sack.rpgroll.structures.deposit.MoneyService;
import com.sack.rpgroll.structures.deposit.ReturnService;
import com.sack.rpgroll.structures.gui.CenterMenu;
import com.sack.rpgroll.structures.gui.PreviewReopenListener;
import com.sack.rpgroll.structures.integration.FloodgateBedrock;
import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.integration.Integrations;
import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.module.FarmModule;
import com.sack.rpgroll.structures.module.MobFarmModule;
import com.sack.rpgroll.structures.module.ParcelClaims;
import com.sack.rpgroll.structures.module.ParcelModule;
import com.sack.rpgroll.structures.module.MythicSpawner;
import com.sack.rpgroll.structures.module.ModuleRegistry;
import com.sack.rpgroll.structures.module.ModuleService;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.preview.PreviewService;
import com.sack.rpgroll.structures.protect.AreaGuard;
import com.sack.rpgroll.structures.protect.AreaGuards;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectService;
import com.sack.rpgroll.structures.project.RepairScans;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import com.sack.rpgroll.structures.storage.WorkbenchRepository;
import com.sack.rpgroll.structures.workbench.BenchOpener;
import com.sack.rpgroll.structures.workbench.BenchProjects;
import com.sack.rpgroll.structures.workbench.SerialSigner;
import com.sack.rpgroll.structures.workbench.ServerSecret;
import com.sack.rpgroll.structures.workbench.WorkbenchBlocks;
import com.sack.rpgroll.structures.workbench.WorkbenchDisplay;
import com.sack.rpgroll.structures.workbench.WorkbenchIssuer;
import com.sack.rpgroll.structures.workbench.WorkbenchItems;
import com.sack.rpgroll.structures.workbench.WorkbenchRecipe;
import com.sack.rpgroll.structures.workbench.WorkbenchService;
import com.sack.rpgroll.structures.workbench.WorkbenchStamp;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import com.sack.rpgroll.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;

/** RPGRoll-Structures: mesas de construcción y estructuras. */
public class StructuresPlugin extends JavaPlugin {

    /** Módulo con el que se registra el fondo de los menús en {@link MenuSkin}. */
    public static final String MENU_MODULE = "structures";

    private LangManager lang;
    private StructuresSettings settings;
    private WorkbenchSettings workbench;
    private CategorySettings categories;
    private PerformanceSettings performance;
    private Dialect dialect;
    private Database database;
    private WorkbenchRepository workbenches;
    private WorkbenchItems items;
    private SerialSigner signer;
    private WorkbenchIssuer issuer;
    private WorkbenchRecipe recipe;
    private WorkbenchService service;
    private ProjectService projects;
    /** Catálogo; lo llena {@link #loadCatalog()} al arrancar y al recargar. */
    private final StructureRegistry structures = new StructureRegistry();
    private final MaterialRegistry materials = new MaterialRegistry();
    /** Módulos funcionales por {@code module.type}; se llenan antes del primer {@link #loadCatalog()}. */
    private final ModuleRegistry moduleTypes = new ModuleRegistry();
    private ModuleService modules;
    private RepairScans repairScans;
    private ProjectRepository projectRepo;
    private ReturnService returns;
    private MoneyService money;
    private DepositService deposits;
    private ConstructionEngine engine;
    /** Protección de la zona entera (WorldGuard, GriefPrevention); la mira también la confirmación de una mejora. */
    private AreaGuard guard;
    private PreviewService preview;
    private WorkbenchBlocks blocks;
    private ChatPromptManager prompts;
    private YamlConfiguration gui;
    private volatile Integrations integrations;
    private MythicSpawner mythicSpawner;

    /** Adaptador de MythicMobs, uno solo (guarda sus métodos resueltos); se crea al primer uso. */
    private MythicSpawner mythicSpawner() {
        if (mythicSpawner == null) {
            org.bukkit.plugin.Plugin mm = Bukkit.getPluginManager().getPlugin(Hook.MYTHICMOBS.plugin());
            mythicSpawner = mm == null ? (n, at) -> null : MythicSpawner.reflective(mm.getClass().getClassLoader(),
                    why -> integrations.markBroken(Hook.MYTHICMOBS, why));
        }
        return mythicSpawner;
    }
    /** Vuelta al hilo principal de los servicios (ver {@link MainQueue}); null hasta onEnable. */
    private MainQueue main;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!libOk()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        main = new MainQueue(getLogger());
        Bukkit.getScheduler().runTaskTimer(this, main::drain, 1L, 1L);
        saveDefaults();
        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        loadConfigs();
        loadIntegrations();
        // Los modelos de las mesas van en el jar (resourcepack/) y se registran en SackResourcePack si está.
        if (com.sack.rpgroll.common.assets.ModuleAssetSync.registerEmbeddedPack(this)) {
            getLogger().info("✔ Modelos registrados en SackResourcePack (aplica con /srp publish).");
        }
        prompts = new ChatPromptManager(this, lang);
        getServer().getPluginManager().registerEvents(prompts, this);

        try {
            database = openDatabase(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "storage.yml")));
            workbenches = new WorkbenchRepository(database, dialect);
        } catch (Exception e) {
            // Cualquier fallo (también un driver ausente o un storage.yml raro): se desactiva limpio, sin medio arranque.
            getLogger().log(Level.SEVERE, "No se pudo abrir la base de datos; RPGRoll-Structures se desactiva.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        signer = new SerialSigner(ServerSecret.load(getDataFolder().toPath().resolve("secret.key")));
        items = new WorkbenchItems(this);
        issuer = new WorkbenchIssuer(workbenches::nextSerial, signer, main, getLogger());
        issuer.refill();
        WorkbenchDisplay display = new WorkbenchDisplay(this, () -> workbench, lang, getLogger());
        projectRepo = new ProjectRepository(database, dialect);
        moduleTypes.register(new FarmModule());
        moduleTypes.register(new MobFarmModule(Bukkit::getWorld, () -> integrations.active(Hook.MYTHICMOBS), this::mythicSpawner));
        Map<Hook, ParcelClaims> parcelClaims = Map.of(Hook.GRIEFPREVENTION, ParcelClaims.griefPrevention(),
                Hook.WORLDGUARD, ParcelClaims.worldGuard());
        moduleTypes.register(new ParcelModule(h -> integrations.active(h), parcelClaims::get,
                (h, why) -> integrations.markBroken(h, why), projectRepo, main::execute, (who, key) -> {
                    Player p = Bukkit.getPlayer(who);
                    if (p != null) {
                        lang.send(p, key);
                    }
                }, getLogger()));
        loadCatalog();
        money = new MoneyService(projectRepo, () -> integrations.active(Hook.VAULT),
                com.sack.rpgroll.common.integration.VaultEconomy::get, Bukkit::getOfflinePlayer, main::execute,
                getLogger());
        projects = new ProjectService(projectRepo, main::execute, getLogger(),
                id -> structures.get(id).map(d -> d.materials()).orElse(List.of()), money);
        projects.load();
        returns = new ReturnService(projectRepo, materials, projects::finish, Bukkit::getPlayer,
                main::execute, (p, key) -> lang.send(p, key), money, getLogger());
        deposits = new DepositService(projectRepo, structures, materials, returns, Bukkit::getPlayer,
                main::execute, getLogger());
        guard = AreaGuards.fromIntegrations(this::integrations);
        BlockWriter writer = BlockWriter.bukkit();
        modules = new ModuleService(moduleTypes, structures::get, projectRepo, writer::chunkLoaded, main::execute,
                getLogger());
        modules.load();
        // La construcción ya está en la base de datos cuando llega aquí: el BuildRecord es el mismo que se insertó
        // (null al terminar una reparación: la construcción ya estaba y su módulo no se vuelve a crear; en una
        // mejora, la nueva, que ya reemplazó a la vieja).
        engine = new ConstructionEngine(projects, projectRepo, structures::get, () -> performance,
                writer, main::execute, (p, built) -> {
                    modules.completed(p, built); // primero: no lanza, y un aviso que falle no deja la granja sin módulo
                    announceCompleted(p);
                }, this::announcePaused,
                System::currentTimeMillis, r -> Bukkit.getScheduler().runTaskAsynchronously(this, r),
                // Mesa cargada: conocida, no huérfana y con su chunk cargado.
                bench -> service.byId(bench).filter(b -> !b.orphan())
                        .map(b -> writer.chunkLoaded(b.world(), b.x(), b.z())).orElse(false),
                guard, modules::precheck, getLogger());
        // Después del motor y de las devoluciones: romper la mesa con pause/cancel y volver a colocarla las usan.
        service = new WorkbenchService(workbenches, items, signer, lang, () -> workbench, () -> settings,
                new BenchProjects(projects, engine, returns, () -> performance.cancelRefundPercent(),
                        Bukkit::getPlayer, (p, key) -> lang.send(p, key)),
                display, name -> Bukkit.getWorld(name) != null, main, getLogger());
        service.load();
        Bukkit.getScheduler().runTaskTimer(this, engine::tick, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(this, modules::tick, 1L, 1L);
        repairScans = new RepairScans(writer);
        Bukkit.getScheduler().runTaskTimer(this, repairScans::tick, 1L, 1L);
        preview = new PreviewService(() -> performance, lang, projectRepo,
                guard, service::byId, new FloodgateBedrock(this::integrations), getLogger());
        getServer().getPluginManager().registerEvents(preview, this);
        getServer().getPluginManager().registerEvents(new PreviewReopenListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, preview::tick, 10L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, preview::scanTick, 1L, 1L);
        blocks = new WorkbenchBlocks(this, service, items, display, lang);
        getServer().getPluginManager().registerEvents(blocks, this);
        benchOpener((player, bench) -> new CenterMenu(player, this, bench).open());
        verifyLoadedChunks();
        // item:/mueble: solo existen cuando RPGRoll-Items/Furniture registran sus Things.Give: un módulo que se
        // habilita después (o sin softdepend) se cubre leyendo el catálogo otra vez con el servidor ya cargado.
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onServerLoad(ServerLoadEvent e) {
                loadIntegrations(); // una economía de Vault que se registra tarde (EssentialsX) ya está
                loadCatalog();
                payPendingOnline();
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                payPending(e.getPlayer());
            }
        }, this);
        payPendingOnline(); // habilitado tarde (sin ServerLoadEvent): los ya conectados

        recipe = new WorkbenchRecipe(this);
        getServer().getPluginManager().registerEvents(recipe, this);
        recipe.reload();

        // Por Brigadier (execute as); los permisos de admin se miran dentro, por subcomando.
        StructuresCommand player = new StructuresCommand(this);
        BrigadierCommands.register(this, "structures", "Ayuda de RPGRoll-Structures", List.of(), player, player,
                "rpgrollstructures.use");
        StructuresAdminCommand admin = new StructuresAdminCommand(this);
        BrigadierCommands.register(this, "structuresadmin", "Administra RPGRoll-Structures", List.of(), admin, admin,
                null);
        // /dar mesa:<nivel>
        Things.give(this, "mesa", () -> List.of("constructor", "ingeniero", "arquitecto"),
                id -> WorkbenchTier.byName(id).flatMap(this::newBench));
    }

    private void saveDefaults() {
        saveDefaultConfig();
        for (String file : List.of("workbench.yml", "storage.yml", "gui.yml", "categories.yml",
                "performance.yml", "integrations.yml")) {
            if (!new File(getDataFolder(), file).exists()) {
                saveResource(file, false);
            }
        }
        // structures/ (un YAML por estructura) y schematics/ (.nbt y .schem).
        new File(getDataFolder(), "structures").mkdirs();
        new File(getDataFolder(), "schematics").mkdirs();
        if (!new File(getDataFolder(), "structures/README.txt").exists()) {
            saveResource("structures/README.txt", false);
        }
    }

    /** Structures 1.1.x necesita RPGRoll-Lib 1.1.14 o más (paquete common.structure). */
    private boolean libOk() {
        var lib = getServer().getPluginManager().getPlugin("RPGRoll-Lib");
        String v = lib == null ? "?" : lib.getPluginMeta().getVersion();
        String[] p = v.split("[.-]");
        int[] min = {1, 1, 14};
        for (int i = 0; i < 3; i++) {
            int n;
            try {
                n = i < p.length ? Integer.parseInt(p[i]) : 0;
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n != min[i]) {
                if (n > min[i]) {
                    return true;
                }
                getLogger().severe("RPGRoll-Structures necesita RPGRoll-Lib 1.1.14 o más; hay " + v
                        + ". Se desactiva.");
                return false;
            }
        }
        return true;
    }

    /**
     * Lee config.yml, workbench.yml, gui.yml, el idioma y el fondo de los menús. No toca storage.yml. Todo se lee
     * antes de asignar nada: si un archivo falla, se queda entera la configuración anterior.
     */
    private void loadConfigs() {
        StructuresSettings newSettings = StructuresSettings.from(getConfig(), getLogger()::warning);
        WorkbenchSettings newWorkbench = WorkbenchSettings.from(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "workbench.yml")),
                getLogger()::warning);
        CategorySettings newCategories = CategorySettings.from(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "categories.yml")),
                getLogger()::warning);
        PerformanceSettings newPerformance = PerformanceSettings.from(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "performance.yml")),
                getLogger()::warning);
        YamlConfiguration newGui = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        MenuSkin newSkin = MenuSkin.from(getConfig().getConfigurationSection("menus"));
        lang.reload(newSettings.language());
        settings = newSettings;
        workbench = newWorkbench;
        categories = newCategories;
        performance = newPerformance;
        gui = newGui;
        MenuSkin.register(MENU_MODULE, newSkin);
    }

    /**
     * Lee integrations.yml y mira qué plugins hay. Se asigna un registro nuevo entero; los adaptadores consultan
     * {@link #integrations()} cada vez, así que una recarga los pone al día.
     */
    private void loadIntegrations() {
        Map<Hook, Supplier<String>> probes = new EnumMap<>(Hook.class);
        probes.put(Hook.VAULT, () -> com.sack.rpgroll.common.integration.VaultEconomy.get().isPresent() ? null
                : Integrations.REASON_NO_ECONOMY);
        integrations = Integrations.load(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "integrations.yml")),
                com.sack.rpgroll.common.integration.SoftDepend::enabled, probes, getLogger());
    }

    /** Dinero pendiente de los jugadores conectados (sin Vault activo no hace nada: sigue pendiente). */
    private void payPendingOnline() {
        if (money.available()) {
            Bukkit.getOnlinePlayers().forEach(this::payPending);
        }
    }

    private void payPending(Player p) {
        // El futuro se completa en el hilo principal.
        money.payPending(p.getUniqueId()).thenAccept(n -> {
            if (n > 0 && p.isOnline()) {
                lang.send(p, "structure.money-paid");
            }
        });
    }

    /** Repasa los chunks cargados: mesas sin bloque, bloques de otra configuración y displays. */
    private void verifyLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                blocks.verify(chunk);
            }
        }
    }

    /**
     * Recarga todo lo que se puede en caliente: ajustes, mesas por nivel, idioma, menús y receta, y repone el bloque
     * de las mesas de los chunks ya cargados si cambió {@code fallback-block} o el modelo.
     * storage.yml NO: cambiar de base de datos pide reiniciar.
     */
    public void reloadAll() {
        saveDefaults();
        reloadConfig();
        loadConfigs();
        loadIntegrations();
        loadCatalog();
        recipe.reload();
        verifyLoadedChunks();
    }

    /** Lee structures/ y schematics/ sin tocar el catálogo en uso (para validar). */
    public StructureLoader.LoadResult scanCatalog() {
        Map<WorkbenchTier, WorkbenchSettings.Size> sizes = new EnumMap<>(WorkbenchTier.class);
        workbench.tiers().forEach((tier, ts) -> sizes.put(tier, ts.maxSize()));
        return StructureLoader.load(new File(getDataFolder(), "structures"), new File(getDataFolder(), "schematics"),
                categories, sizes, materials::knows, f -> {
                    try {
                        return BlockSources.read(f);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, moduleTypes::get);
    }

    /** Carga el catálogo y lo cambia entero; los problemas solo van al log (y a /structuresadmin validate). */
    public void loadCatalog() {
        materials.clearCache();
        StructureLoader.LoadResult r = scanCatalog();
        structures.replace(r);
        getLogger().info("Catálogo: " + r.valid().size() + " estructura(s), " + r.problems().size()
                + " con problemas.");
        r.problems().forEach(pr -> getLogger().warning(pr.file() + " [" + pr.key() + "]: " + pr.reason()));
    }

    /** Un ítem de mesa firmado con la identidad dada. */
    public ItemStack itemFor(WorkbenchStamp stamp) {
        return items.create(stamp, lang, workbench.tiers().get(stamp.tier()));
    }

    /** Una mesa nueva ya, sin bloquear (series reservadas); vacío si no hay series listas. */
    public Optional<ItemStack> newBench(WorkbenchTier tier) {
        return issuer.issueNow(tier).map(this::itemFor);
    }

    public WorkbenchIssuer issuer() {
        return issuer;
    }

    /** Obra pausada por el motor: el motivo ({@link Project#error()}) es la clave del aviso al dueño. */
    private void announcePaused(Project p) {
        Player owner = Bukkit.getPlayer(p.owner());
        if (owner != null && p.error() != null) {
            lang.send(owner, p.error());
        }
    }

    /** Obra terminada: aviso al dueño si está conectado, partículas y sonido en la mesa. */
    private void announceCompleted(Project p) {
        Player owner = Bukkit.getPlayer(p.owner());
        if (owner != null) {
            lang.send(owner, "build.completed", "structure",
                    structures.get(p.structure()).map(d -> d.name(settings.language())).orElse(p.structure()));
        }
        service.byId(p.bench()).ifPresent(b -> {
            World w = Bukkit.getWorld(b.world());
            if (w != null) {
                Location at = new Location(w, b.x() + 0.5, b.y() + 1.0, b.z() + 0.5);
                w.spawnParticle(Particle.HAPPY_VILLAGER, at, 20, 0.5, 0.5, 0.5);
                w.playSound(at, Sound.BLOCK_ANVIL_USE, 1f, 1f);
            }
        });
    }

    @Override
    public void onDisable() {
        // Cada paso aislado: un fallo no impide los siguientes, y todo tolera un arranque a medias (nulls).
        step("vistas previas", () -> {
            if (preview != null) {
                preview.stopAll();
            }
        });
        step("motor de obras", () -> {
            if (engine != null) {
                engine.flushAll(); // antes de cerrar la base de datos
            }
        });
        // Lo que la base de datos ya confirmó (recuperar, cancelar, recoger, depositar) se entrega aquí: runTask ya
        // no corre. Barrera + vaciado, varias vueltas (una entrega puede escribir pendientes).
        step("entregas en vuelo", () -> {
            if (main != null && database != null) {
                main.drainAtShutdown(() -> database.submit(c -> null), 5_000, 10);
            }
        });
        step("receta", () -> {
            if (recipe != null) {
                recipe.unregister();
            }
        });
        step("base de datos", () -> {
            if (database != null) {
                database.close();
            }
        });
    }

    private void step(String what, Runnable r) {
        try {
            r.run();
        } catch (Throwable e) {
            getLogger().log(Level.SEVERE, "Al apagar (" + what + ")", e);
        }
    }

    private Database openDatabase(YamlConfiguration storage) throws SQLException {
        if ("mysql".equalsIgnoreCase(storage.getString("type", "sqlite"))) {
            dialect = Dialect.MYSQL;
            String url = Database.mysqlUrl(storage.getString("mysql.host", "localhost"),
                    storage.getInt("mysql.port", 3306), storage.getString("mysql.database", "rpgrollstructures"));
            return Database.open(dialect, url, storage.getString("mysql.user", "root"),
                    storage.getString("mysql.password", ""), getLogger());
        }
        dialect = Dialect.SQLITE;
        File file = new File(getDataFolder(), storage.getString("sqlite.file", "data.db"));
        return Database.open(dialect, "jdbc:sqlite:" + file.getAbsolutePath(), "", "", getLogger());
    }

    public WorkbenchRepository workbenches() {
        return workbenches;
    }

    public WorkbenchItems items() {
        return items;
    }

    public SerialSigner signer() {
        return signer;
    }

    public PreviewService preview() {
        return preview;
    }

    public StructureRegistry structures() {
        return structures;
    }

    public MaterialRegistry materials() {
        return materials;
    }

    public ProjectRepository projectRepository() {
        return projectRepo;
    }

    public DepositService deposits() {
        return deposits;
    }

    public ReturnService returns() {
        return returns;
    }

    public MoneyService money() {
        return money;
    }

    /** Lecturas del mundo de «Reparar», repartidas en ticks. */
    public RepairScans repairScans() {
        return repairScans;
    }

    public ModuleService modules() {
        return modules;
    }

    public AreaGuard guard() {
        return guard;
    }

    public ConstructionEngine engine() {
        return engine;
    }

    public ProjectService projects() {
        return projects;
    }

    public WorkbenchService service() {
        return service;
    }

    public ChatPromptManager prompts() {
        return prompts;
    }

    /**
     * Título de un menú: {@code <menu>.title} de gui.yml. Un valor que empieza por {@code menu.} es una clave del
     * lang (traducido); cualquier otro texto se usa tal cual.
     */
    public Component menuTitle(String menu) {
        String value = gui.getString(menu + ".title", "menu." + menu + ".title");
        return value.startsWith("menu.") ? lang.component(value) : ComponentUtils.parse(value);
    }

    /** Enchufa el menú que se abre al usar una mesa. */
    public void benchOpener(BenchOpener opener) {
        blocks.opener(opener);
    }

    public StructuresSettings settings() {
        return settings;
    }

    public WorkbenchSettings workbench() {
        return workbench;
    }

    public CategorySettings categories() {
        return categories;
    }

    public PerformanceSettings performance() {
        return performance;
    }

    public Integrations integrations() {
        return integrations;
    }

    public LangManager lang() {
        return lang;
    }
}
