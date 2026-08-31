package com.sack.rpgroll.tab;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.tab.animation.AnimationEngine;
import com.sack.rpgroll.tab.animation.AnimationManager;
import com.sack.rpgroll.tab.api.TABAPI;
import com.sack.rpgroll.tab.belowname.BelowNameEngine;
import com.sack.rpgroll.tab.belowname.BelowNameManager;
import com.sack.rpgroll.tab.bossbar.BossBarEngine;
import com.sack.rpgroll.tab.bossbar.BossBarManager;
import com.sack.rpgroll.tab.command.TabAdminCommand;
import com.sack.rpgroll.tab.context.ContextManager;
import com.sack.rpgroll.tab.context.ContextResolver;
import com.sack.rpgroll.tab.core.RefreshCoordinator;
import com.sack.rpgroll.tab.integration.ProtocolLibHook;
import com.sack.rpgroll.tab.listener.PlayerLifecycleListener;
import com.sack.rpgroll.tab.nametag.NametagEngine;
import com.sack.rpgroll.tab.nametag.NametagManager;
import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.ProfileManager;
import com.sack.rpgroll.tab.scoreboard.LineConditionEvaluator;
import com.sack.rpgroll.tab.scoreboard.ScoreboardEngine;
import com.sack.rpgroll.tab.scoreboard.ScoreboardManager;
import com.sack.rpgroll.tab.sorting.SortingEngine;
import com.sack.rpgroll.tab.sorting.SortingManager;
import com.sack.rpgroll.tab.tablist.TabListEngine;
import com.sack.rpgroll.tab.tablist.TablistManager;
import com.sack.rpgroll.tab.teams.PlayerScoreboardService;
import com.sack.rpgroll.tab.teams.TeamsEngine;
import com.sack.rpgroll.tab.teams.TeamsManager;
import com.sack.rpgroll.tab.visibility.VisibilityEngine;
import com.sack.rpgroll.tab.visibility.WorldVisibilityService;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TabPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of(
            "profiles", "contexts", "animations", "tablists", "sortings", "teams", "nametags", "belownames",
            "scoreboards", "bossbars");

    private static final long ANIMATION_REFRESH_TICKS = 10L;

    private LangManager langManager;

    private PlaceholderEngine placeholderEngine;
    private AnimationEngine animationEngine;
    private AnimationManager animationManager;
    private ContextManager contextManager;
    private ContextResolver contextResolver;
    private ProfileManager profileManager;
    private PlayerStateManager playerStateManager;

    private TablistManager tablistManager;
    private SortingManager sortingManager;
    private SortingEngine sortingEngine;
    private TeamsManager teamsManager;
    private PlayerScoreboardService playerScoreboardService;
    private TeamsEngine teamsEngine;
    private TabListEngine tabListEngine;

    private NametagManager nametagManager;
    private NametagEngine nametagEngine;
    private BelowNameManager belowNameManager;
    private BelowNameEngine belowNameEngine;
    private ScoreboardManager scoreboardManager;
    private ScoreboardEngine scoreboardEngine;
    private BossBarManager bossBarManager;
    private BossBarEngine bossBarEngine;

    private VisibilityEngine visibilityEngine;
    private WorldVisibilityService worldVisibilityService;

    private RefreshCoordinator refreshCoordinator;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        placeholderEngine = new PlaceholderEngine(this);

        animationManager = new AnimationManager(this);
        animationManager.initialize();
        animationEngine = new AnimationEngine(this);
        animationEngine.start();

        contextManager = new ContextManager(this);
        contextManager.initialize();
        contextResolver = new ContextResolver(contextManager, placeholderEngine);

        profileManager = new ProfileManager(this);
        profileManager.initialize();
        playerStateManager = new PlayerStateManager(contextResolver, profileManager);

        tablistManager = new TablistManager(this);
        tablistManager.initialize();
        sortingManager = new SortingManager(this);
        sortingManager.initialize();
        sortingEngine = new SortingEngine(placeholderEngine);
        teamsManager = new TeamsManager(this);
        teamsManager.initialize();
        playerScoreboardService = new PlayerScoreboardService();
        teamsEngine = new TeamsEngine(playerScoreboardService);
        tabListEngine = new TabListEngine(tablistManager, sortingManager, sortingEngine, teamsManager, teamsEngine,
                placeholderEngine, animationManager, animationEngine, playerStateManager);

        nametagManager = new NametagManager(this);
        nametagManager.initialize();
        nametagEngine = new NametagEngine(this, placeholderEngine);

        belowNameManager = new BelowNameManager(this);
        belowNameManager.initialize();
        belowNameEngine = new BelowNameEngine(playerScoreboardService, placeholderEngine);

        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.initializeAndResolve();
        LineConditionEvaluator conditionEvaluator = new LineConditionEvaluator(placeholderEngine);
        scoreboardEngine = new ScoreboardEngine(playerScoreboardService, placeholderEngine, conditionEvaluator,
                animationManager, animationEngine);

        bossBarManager = new BossBarManager(this);
        bossBarManager.initialize();
        bossBarEngine = new BossBarEngine(placeholderEngine);

        visibilityEngine = new VisibilityEngine();
        worldVisibilityService = new WorldVisibilityService(visibilityEngine, playerStateManager, tablistManager);

        refreshCoordinator = new RefreshCoordinator(playerStateManager, tabListEngine, nametagManager, nametagEngine,
                belowNameManager, belowNameEngine, scoreboardManager, scoreboardEngine, bossBarManager, bossBarEngine,
                worldVisibilityService);

        TABAPI.init(refreshCoordinator, playerStateManager, profileManager, contextManager, scoreboardManager,
                tablistManager, bossBarManager, placeholderEngine);

        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(refreshCoordinator), this);

        registerCommand();

        getServer().getScheduler().runTaskTimer(this, refreshCoordinator::tickAnimations,
                ANIMATION_REFRESH_TICKS, ANIMATION_REFRESH_TICKS);

        refreshCoordinator.refreshAll();

        getLogger().info("✔ RPGRoll-TAB habilitado. " + profileManager.count() + " perfil(es), "
                + contextManager.count() + " contexto(s), " + tablistManager.count() + " tablist(s), "
                + scoreboardManager.count() + " scoreboard(s)."
                + (ProtocolLibHook.isPresent() ? "" : " (ProtocolLib no detectado — visibilidad por-viewer deshabilitada)"));
    }

    @Override
    public void onDisable() {

        if (animationEngine != null) {
            animationEngine.stop();
        }
    }

    private void registerCommand() {

        var tabCommand = getCommand("tabadmin");

        if (tabCommand == null) {
            getLogger().severe("✘ El comando 'tabadmin' no está declarado en plugin.yml");
            return;
        }

        var executor = new TabAdminCommand(profileManager, contextManager, tablistManager, scoreboardManager,
                nametagManager, belowNameManager, bossBarManager, sortingManager, teamsManager, animationManager,
                playerStateManager, refreshCoordinator, langManager);

        tabCommand.setExecutor(executor);
        tabCommand.setTabCompleter(executor);
    }

}
