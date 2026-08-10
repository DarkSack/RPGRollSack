package com.sack.rpgroll.tab.api;

import com.sack.rpgroll.tab.bossbar.BossBarManager;
import com.sack.rpgroll.tab.context.ContextManager;
import com.sack.rpgroll.tab.core.RefreshCoordinator;
import com.sack.rpgroll.tab.placeholder.TABPlaceholderRegistry;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.ProfileManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.scoreboard.ScoreboardManager;
import com.sack.rpgroll.tab.tablist.TablistManager;

import org.bukkit.entity.Player;

/**
 * Punto de entrada público de RPGRoll-TAB (sección 43 del diseño). Otros
 * addons (Guilds, Ascension, Dungeons, Economy, Magic...) entran por acá
 * para registrar sus propios placeholders, reaccionar a cambios de perfil,
 * o forzar un perfil/evento temporal — RPGRoll-TAB nunca los conoce a ellos.
 */
public final class TABAPI {

    private static TABAPI instance;

    private final RefreshCoordinator refreshCoordinator;
    private final PlayerStateManager playerStateManager;
    private final ProfileManager profileManager;
    private final ContextManager contextManager;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;
    private final BossBarManager bossBarManager;
    private final TABPlaceholderRegistry placeholderRegistry;

    private TABAPI(RefreshCoordinator refreshCoordinator, PlayerStateManager playerStateManager,
            ProfileManager profileManager, ContextManager contextManager, ScoreboardManager scoreboardManager,
            TablistManager tablistManager, BossBarManager bossBarManager,
            TABPlaceholderRegistry placeholderRegistry) {
        this.refreshCoordinator = refreshCoordinator;
        this.playerStateManager = playerStateManager;
        this.profileManager = profileManager;
        this.contextManager = contextManager;
        this.scoreboardManager = scoreboardManager;
        this.tablistManager = tablistManager;
        this.bossBarManager = bossBarManager;
        this.placeholderRegistry = placeholderRegistry;
    }

    public static void init(RefreshCoordinator refreshCoordinator, PlayerStateManager playerStateManager,
            ProfileManager profileManager, ContextManager contextManager, ScoreboardManager scoreboardManager,
            TablistManager tablistManager, BossBarManager bossBarManager,
            TABPlaceholderRegistry placeholderRegistry) {
        instance = new TABAPI(refreshCoordinator, playerStateManager, profileManager, contextManager,
                scoreboardManager, tablistManager, bossBarManager, placeholderRegistry);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-TAB todavía no está listo. */
    public static TABAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-TAB todavía no está listo.");
        }

        return instance;
    }

    public PlayerTabHandle player(Player player) {
        return new PlayerTabHandle(player, refreshCoordinator, playerStateManager, profileManager);
    }

    public TABPlaceholderRegistry placeholders() {
        return placeholderRegistry;
    }

    public ProfileManager profiles() {
        return profileManager;
    }

    public ContextManager contexts() {
        return contextManager;
    }

    public ScoreboardManager scoreboards() {
        return scoreboardManager;
    }

    public TablistManager tablists() {
        return tablistManager;
    }

    public BossBarManager bossBars() {
        return bossBarManager;
    }

    /** Registra un perfil en memoria (no persiste a YAML) — para eventos temporales (sección 42). */
    public void registerProfile(String id, TABProfile profile) {
        playerStateManager.registerRuntimeProfile(profile);
    }

    /** Activa un perfil para TODO el servidor hasta {@link #deactivateEventProfile()} — ej. un evento de temporada. */
    public void activateProfile(String id) {
        playerStateManager.setGlobalOverride(id);
        refreshCoordinator.refreshAll();
    }

    public void deactivateEventProfile() {
        playerStateManager.clearGlobalOverride();
        refreshCoordinator.refreshAll();
    }

}
