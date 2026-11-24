package com.sack.rpgroll.structures;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.gui.ChatPromptManager;
import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.structures.command.StructuresAdminCommand;
import com.sack.rpgroll.structures.command.StructuresCommand;
import com.sack.rpgroll.structures.config.StructuresSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.gui.CenterMenu;
import com.sack.rpgroll.structures.storage.Database;
import com.sack.rpgroll.structures.storage.Dialect;
import com.sack.rpgroll.structures.storage.WorkbenchRepository;
import com.sack.rpgroll.structures.workbench.BenchOpener;
import com.sack.rpgroll.structures.workbench.ProjectsView;
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
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/** RPGRoll-Structures: mesas de construcción y estructuras. */
public class StructuresPlugin extends JavaPlugin {

    /** Módulo con el que se registra el fondo de los menús en {@link MenuSkin}. */
    public static final String MENU_MODULE = "structures";

    private LangManager lang;
    private StructuresSettings settings;
    private WorkbenchSettings workbench;
    private Dialect dialect;
    private Database database;
    private WorkbenchRepository workbenches;
    private WorkbenchItems items;
    private SerialSigner signer;
    private WorkbenchIssuer issuer;
    private WorkbenchRecipe recipe;
    private WorkbenchService service;
    private WorkbenchBlocks blocks;
    private ChatPromptManager prompts;
    private YamlConfiguration gui;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaults();
        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        loadConfigs();
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
        issuer = new WorkbenchIssuer(workbenches::nextSerial, signer, r -> Bukkit.getScheduler().runTask(this, r),
                getLogger());
        issuer.refill();
        WorkbenchDisplay display = new WorkbenchDisplay(this, () -> workbench, lang, getLogger());
        service = new WorkbenchService(workbenches, items, signer, lang, () -> workbench, () -> settings,
                ProjectsView.none(), display, name -> Bukkit.getWorld(name) != null,
                r -> Bukkit.getScheduler().runTask(this, r), getLogger());
        service.load();
        blocks = new WorkbenchBlocks(this, service, items, display, lang);
        getServer().getPluginManager().registerEvents(blocks, this);
        benchOpener((player, bench) -> new CenterMenu(player, this, bench).open());
        verifyLoadedChunks();

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
        for (String file : List.of("workbench.yml", "storage.yml", "gui.yml")) {
            if (!new File(getDataFolder(), file).exists()) {
                saveResource(file, false);
            }
        }
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
        YamlConfiguration newGui = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        MenuSkin newSkin = MenuSkin.from(getConfig().getConfigurationSection("menus"));
        lang.reload(newSettings.language());
        settings = newSettings;
        workbench = newWorkbench;
        gui = newGui;
        MenuSkin.register(MENU_MODULE, newSkin);
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
        recipe.reload();
        verifyLoadedChunks();
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

    @Override
    public void onDisable() {
        if (recipe != null) {
            recipe.unregister();
        }
        if (database != null) {
            database.close();
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

    public LangManager lang() {
        return lang;
    }
}
