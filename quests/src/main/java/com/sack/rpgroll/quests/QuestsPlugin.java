package com.sack.rpgroll.quests;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.quests.command.QuestAdminCommand;
import com.sack.rpgroll.quests.command.QuestCommand;
import com.sack.rpgroll.quests.condition.QuestConditionEvaluator;
import com.sack.rpgroll.quests.core.QuestManager;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.engine.QuestPollingTask;
import com.sack.rpgroll.quests.gui.ChatPromptManager;
import com.sack.rpgroll.quests.integration.QuestsPlaceholders;
import com.sack.rpgroll.quests.listener.QuestObjectiveListener;
import com.sack.rpgroll.quests.player.QuestPlayerStateManager;
import com.sack.rpgroll.quests.region.RegionManager;
import com.sack.rpgroll.quests.registry.ActionRegistry;
import com.sack.rpgroll.quests.registry.BuiltinActions;
import com.sack.rpgroll.quests.registry.ConditionRegistry;
import com.sack.rpgroll.quests.registry.ObjectiveTypeRegistry;
import com.sack.rpgroll.quests.requirement.QuestRequirementChecker;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class QuestsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("quests", "regions");
    private static final long POLL_INTERVAL_TICKS = 20L;

    private QuestManager questManager;
    private RegionManager regionManager;
    private ObjectiveTypeRegistry objectiveTypeRegistry;
    private ActionRegistry actionRegistry;
    private ConditionRegistry conditionRegistry;
    private QuestEngine questEngine;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        questManager = new QuestManager(this);
        regionManager = new RegionManager(this);
        objectiveTypeRegistry = new ObjectiveTypeRegistry();
        conditionRegistry = new ConditionRegistry();
        actionRegistry = new ActionRegistry(this);

        regionManager.initialize();
        questManager.initialize();

        QuestPlayerStateManager stateManager = new QuestPlayerStateManager(this);
        QuestRequirementChecker requirementChecker = new QuestRequirementChecker(regionManager, langManager);
        QuestConditionEvaluator conditionEvaluator = new QuestConditionEvaluator(conditionRegistry);

        questEngine = new QuestEngine(this, questManager, stateManager, requirementChecker, conditionEvaluator,
                actionRegistry, langManager);

        BuiltinActions.registerAll(actionRegistry, this, questEngine);

        getServer().getPluginManager().registerEvents(new QuestObjectiveListener(questEngine, langManager), this);

        getServer().getScheduler().runTaskTimer(this, new QuestPollingTask(questEngine, regionManager),
                POLL_INTERVAL_TICKS, POLL_INTERVAL_TICKS);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        registerCommand("quest", new QuestCommand(questEngine, langManager));
        registerCommand("questadmin",
                new QuestAdminCommand(this, questEngine, regionManager, chatPromptManager, langManager));

        registerPlaceholders(stateManager);

        getLogger().info("✔ RPGRoll-Quests habilitado. " + questManager.count() + " misión(es), "
                + regionManager.count() + " región(es) cargadas.");
    }

    private void registerPlaceholders(QuestPlayerStateManager stateManager) {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new QuestsPlaceholders(this, stateManager, questManager).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollquests_...%)");
    }

    @Override
    public void onDisable() {

        if (questEngine != null) {
            questEngine.getStateManager().saveAll();
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
            case "quest" -> new String[] { "Comandos de misiones para jugadores", "" };
            case "questadmin" -> new String[] { "Comandos administrativos de misiones", "rpgrollquests.admin.*" };
            default -> new String[] { "", "" };
        };
    }

    /** API pública: otros plugins/addons pueden registrar quests, tipos de objetivo, condiciones y acciones. */
    public QuestManager getQuestManager() {
        return questManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public ObjectiveTypeRegistry getObjectiveTypeRegistry() {
        return objectiveTypeRegistry;
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public ConditionRegistry getConditionRegistry() {
        return conditionRegistry;
    }

    public QuestEngine getQuestEngine() {
        return questEngine;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
