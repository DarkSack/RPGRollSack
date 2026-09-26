package com.sack.rpgroll.items;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.items.command.ItemAdminCommand;
import com.sack.rpgroll.items.command.ItemCommand;
import com.sack.rpgroll.items.condition.ItemConditionEvaluator;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.durability.DurabilityService;
import com.sack.rpgroll.items.gui.ChatPromptManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.integration.ItemsPlaceholders;
import com.sack.rpgroll.items.listener.AutoRepairTask;
import com.sack.rpgroll.items.listener.ItemEquipTask;
import com.sack.rpgroll.items.listener.ItemTriggerListener;
import com.sack.rpgroll.items.listener.PlayerSessionListener;
import com.sack.rpgroll.items.ore.OreGenerator;
import com.sack.rpgroll.items.ore.OreListener;
import com.sack.rpgroll.items.ore.OreManager;
import com.sack.rpgroll.items.ore.OreService;
import com.sack.rpgroll.items.pack.PackAssetSync;
import com.sack.rpgroll.items.pack.PackManager;
import com.sack.rpgroll.items.rarity.RarityManager;
import com.sack.rpgroll.items.recipe.CustomRecipeRegistry;
import com.sack.rpgroll.items.recipe.RecipeRegistrar;
import com.sack.rpgroll.items.registry.ActionRegistry;
import com.sack.rpgroll.items.registry.BuiltinItemActions;
import com.sack.rpgroll.items.registry.ConditionRegistry;
import com.sack.rpgroll.items.registry.StatRegistry;
import com.sack.rpgroll.items.requirement.ItemRequirementChecker;
import com.sack.rpgroll.items.skin.SkinService;
import com.sack.rpgroll.items.socket.GemItem;
import com.sack.rpgroll.items.socket.GemManager;
import com.sack.rpgroll.items.socket.SocketService;
import com.sack.rpgroll.items.stat.ItemStatEngine;
import com.sack.rpgroll.items.upgrade.UpgradeService;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ItemsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("packs", "rarities", "gems");
    private static final long EQUIP_POLL_TICKS = 10L;
    private static final long AUTO_REPAIR_TICKS = 1200L;

    private ItemManager itemManager;
    private RarityManager rarityManager;
    private GemManager gemManager;
    private GemItem gemItem;
    private OreManager oreManager;
    private OreService oreService;
    private OreListener oreListener;
    private ItemFactory itemFactory;
    private ItemInstanceService instanceService;
    private StatRegistry statRegistry;
    private ActionRegistry actionRegistry;
    private ConditionRegistry conditionRegistry;
    private ItemStatEngine statEngine;
    private SocketService socketService;
    private UpgradeService upgradeService;
    private SkinService skinService;
    private DurabilityService durabilityService;
    private CustomRecipeRegistry customRecipeRegistry;
    private LangManager langManager;
    private PackManager packManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        packManager = new PackManager(this);
        packManager.migrateLegacyItemsFolder();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        new PackAssetSync(this, packManager).syncAll();

        rarityManager = new RarityManager(this);
        gemManager = new GemManager(this);
        itemManager = new ItemManager(this);

        rarityManager.initialize();
        gemManager.initialize();
        itemManager.initialize();

        new java.io.File(getDataFolder(), "ores").mkdirs();
        oreManager = new OreManager(this);
        oreManager.initialize();

        instanceService = new ItemInstanceService(this);
        itemFactory = new ItemFactory(this, instanceService, rarityManager, langManager);

        statRegistry = new StatRegistry();
        conditionRegistry = new ConditionRegistry();
        actionRegistry = new ActionRegistry(this);
        BuiltinItemActions.registerAll(actionRegistry, this);

        ItemConditionEvaluator conditionEvaluator = new ItemConditionEvaluator(conditionRegistry);

        gemItem = new GemItem(this, langManager);
        socketService = new SocketService(instanceService, itemFactory, gemManager, gemItem);
        upgradeService = new UpgradeService(instanceService, itemFactory);
        skinService = new SkinService(instanceService, itemFactory);
        durabilityService = new DurabilityService(instanceService, itemFactory, langManager);

        statEngine = new ItemStatEngine(this, itemManager, itemFactory, instanceService, socketService);

        customRecipeRegistry = new CustomRecipeRegistry();
        new RecipeRegistrar(this, itemFactory).registerAll(itemManager);
        getServer().getPluginManager().registerEvents(new com.sack.rpgroll.items.recipe.RecipeGuardListener(this,
                instanceService, () -> getConfig().getBoolean("recipes.protect-custom-items", true)), this);

        oreService = new OreService(this, oreManager, itemManager, instanceService);
        oreService.rebuild();
        oreListener = new OreListener(this, oreService, langManager);
        getServer().getPluginManager().registerEvents(oreListener, this);
        getServer().getPluginManager().registerEvents(new OreGenerator(this, oreService,
                () -> java.util.Set.copyOf(getConfig().getStringList("ores.retrogen-worlds"))), this);

        ItemRequirementChecker requirementChecker = new ItemRequirementChecker(langManager);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        ItemTriggerListener triggerListener = new ItemTriggerListener(itemManager, instanceService, actionRegistry,
                conditionEvaluator, durabilityService, statEngine, langManager);
        getServer().getPluginManager().registerEvents(triggerListener, this);

        ItemEquipTask equipTask = new ItemEquipTask(itemManager, instanceService, actionRegistry, statEngine,
                requirementChecker, langManager);
        getServer().getScheduler().runTaskTimer(this, equipTask, EQUIP_POLL_TICKS, EQUIP_POLL_TICKS);

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(equipTask), this);

        AutoRepairTask autoRepairTask = new AutoRepairTask(itemManager, instanceService, durabilityService);
        getServer().getScheduler().runTaskTimer(this, autoRepairTask, AUTO_REPAIR_TICKS, AUTO_REPAIR_TICKS);

        registerCommand("itemadmin", new ItemAdminCommand(itemManager, itemFactory, rarityManager, statRegistry,
                chatPromptManager, packManager, this));
        registerCommand("item", new ItemCommand(itemManager, instanceService, upgradeService, skinService,
                socketService, langManager));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Items habilitado. " + itemManager.count() + " ítem(s), "
                + rarityManager.count() + " rareza(s), " + gemManager.count() + " gema(s) cargadas.");
    }

    @Override
    public void onDisable() {
        if (oreListener != null) {
            getServer().getOnlinePlayers().forEach(oreListener::clear);
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {

        String[] info = metaFor(name);

        com.sack.rpgroll.common.command.BrigadierCommands.register(this, name, info[0],
                info[1].isEmpty() ? null : info[1], executor);
    }

    /**
     * Descripción y permiso de cada comando.
     * <p>
     * Vivían en {@code plugin.yml}, pero los comandos se registran por
     * Brigadier (ver {@code BrigadierCommands}) para que {@code execute as}
     * entregue al jugador real, y ahí ya no hay YAML de donde leerlos.
     */
    private String[] metaFor(String name) {

        return switch (name) {
            case "itemadmin" -> new String[] { "Gestiona definiciones de ítems (crear, dar, editar, recargar)", "rpgrollitems.admin.*" };
            case "item" -> new String[] { "Interactúa con el ítem RPGRoll que tienes en mano", "" };
            default -> new String[] { "", "" };
        };
    }

    private void registerPlaceholders() {

        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }

        new ItemsPlaceholders(this, itemManager, instanceService, statEngine).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollitems_...%)");
    }

    // ============ API pública para otros addons ============

    public ItemManager getItemManager() {
        return itemManager;
    }

    public RarityManager getRarityManager() {
        return rarityManager;
    }

    public GemManager getGemManager() {
        return gemManager;
    }

    public OreManager getOreManager() {
        return oreManager;
    }

    public OreService getOreService() {
        return oreService;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    /**
     * Crea un ítem a partir de una referencia de otro addon: el id de un ítem
     * o {@code gem:<id>} para una gema de engaste. Vacío si no existe.
     */
    public java.util.Optional<org.bukkit.inventory.ItemStack> create(String reference, int amount) {

        if (reference == null || reference.isBlank()) {
            return java.util.Optional.empty();
        }

        String ref = reference.trim();
        java.util.Optional<org.bukkit.inventory.ItemStack> item;

        if (ref.regionMatches(true, 0, GEM_PREFIX, 0, GEM_PREFIX.length())) {
            item = gemManager.get(ref.substring(GEM_PREFIX.length()).trim()).map(gemItem::create);
        } else {
            item = itemManager.get(ref).map(itemFactory::create);
        }

        item.ifPresent(stack -> stack.setAmount(Math.max(1, Math.min(amount, stack.getMaxStackSize()))));
        return item;
    }

    /** Prefijo de las referencias a gemas de engaste en loot, tiendas y /itemadmin give. */
    public static final String GEM_PREFIX = "gem:";

    public ItemInstanceService getInstanceService() {
        return instanceService;
    }

    public StatRegistry getStatRegistry() {
        return statRegistry;
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public ConditionRegistry getConditionRegistry() {
        return conditionRegistry;
    }

    public ItemStatEngine getStatEngine() {
        return statEngine;
    }

    public CustomRecipeRegistry getCustomRecipeRegistry() {
        return customRecipeRegistry;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public PackManager getPackManager() {
        return packManager;
    }

}
