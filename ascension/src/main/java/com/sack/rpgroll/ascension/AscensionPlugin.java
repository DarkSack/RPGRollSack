package com.sack.rpgroll.ascension;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.ascension.command.AscendAdminCommand;
import com.sack.rpgroll.ascension.command.AscendCommand;
import com.sack.rpgroll.ascension.core.AffinityManager;
import com.sack.rpgroll.ascension.core.ClassSpecializationManager;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.ascension.core.RaceEvolutionManager;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
import com.sack.rpgroll.ascension.deferred.LegacyManager;
import com.sack.rpgroll.ascension.deferred.SecretUnlockManager;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.ascension.gui.ChatPromptManager;
import com.sack.rpgroll.ascension.integration.AscensionPlaceholders;
import com.sack.rpgroll.ascension.listener.AffinityCombatListener;
import com.sack.rpgroll.ascension.listener.PlayerSessionListener;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.requirement.AscensionRequirementChecker;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class AscensionPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of(
            "evolutions", "specializations", "prestige", "affinities",
            "job-evolutions", "secrets", "factions", "achievements", "titles", "legacy");

    private RaceEvolutionManager evolutionManager;
    private ClassSpecializationManager specializationManager;
    private PrestigeManager prestigeManager;
    private AffinityManager affinityManager;
    private JobEvolutionManager jobEvolutionManager;
    private SecretUnlockManager secretUnlockManager;
    private FactionManager factionManager;
    private AchievementManager achievementManager;
    private TitleManager titleManager;
    private LegacyManager legacyManager;
    private AscensionEngine engine;
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

        evolutionManager = new RaceEvolutionManager(this);
        specializationManager = new ClassSpecializationManager(this);
        prestigeManager = new PrestigeManager(this);
        affinityManager = new AffinityManager(this);
        jobEvolutionManager = new JobEvolutionManager(this);
        secretUnlockManager = new SecretUnlockManager(this);
        factionManager = new FactionManager(this);
        achievementManager = new AchievementManager(this);
        titleManager = new TitleManager(this);
        legacyManager = new LegacyManager(this);

        evolutionManager.initialize();
        specializationManager.initialize();
        prestigeManager.initialize();
        affinityManager.initialize();
        jobEvolutionManager.initialize();
        secretUnlockManager.initialize();
        factionManager.initialize();
        achievementManager.initialize();
        titleManager.initialize();
        legacyManager.initialize();

        AscensionPlayerStateManager stateManager = new AscensionPlayerStateManager(this);
        AscensionRequirementChecker requirementChecker = new AscensionRequirementChecker(langManager);

        engine = new AscensionEngine(this, stateManager, evolutionManager, specializationManager, prestigeManager,
                legacyManager, requirementChecker, langManager);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        getServer().getPluginManager().registerEvents(new PlayerSessionListener(engine), this);
        getServer().getPluginManager().registerEvents(
                new AffinityCombatListener(affinityManager, stateManager), this);

        registerCommand("ascend", new AscendCommand(engine, factionManager, titleManager, langManager));
        registerCommand("ascendadmin", new AscendAdminCommand(engine, achievementManager, titleManager,
                affinityManager, jobEvolutionManager, secretUnlockManager, factionManager, chatPromptManager,
                langManager, this));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Ascension habilitado. " + evolutionManager.count() + " evolución(es), "
                + specializationManager.count() + " especialización(es), " + affinityManager.count()
                + " afinidad(es), " + prestigeManager.count() + " rango(s) de prestigio.");
    }

    private void registerPlaceholders() {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new AscensionPlaceholders(this, engine).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollascension_...%)");
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.getStateManager().saveAll();
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
            case "ascend" -> new String[] { "Comandos de progresión avanzada para jugadores", "" };
            case "ascendadmin" -> new String[] { "Comandos administrativos de Ascension", "rpgrollascension.admin.*" };
            default -> new String[] { "", "" };
        };
    }

    // ============ API pública para otros addons ============

    public AscensionEngine getEngine() {
        return engine;
    }

    public RaceEvolutionManager getEvolutionManager() {
        return evolutionManager;
    }

    public ClassSpecializationManager getSpecializationManager() {
        return specializationManager;
    }

    public PrestigeManager getPrestigeManager() {
        return prestigeManager;
    }

    public AffinityManager getAffinityManager() {
        return affinityManager;
    }

    public JobEvolutionManager getJobEvolutionManager() {
        return jobEvolutionManager;
    }

    public SecretUnlockManager getSecretUnlockManager() {
        return secretUnlockManager;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public LegacyManager getLegacyManager() {
        return legacyManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}
