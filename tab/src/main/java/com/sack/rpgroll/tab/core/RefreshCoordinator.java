package com.sack.rpgroll.tab.core;

import com.sack.rpgroll.tab.belowname.BelowNameEngine;
import com.sack.rpgroll.tab.belowname.BelowNameManager;
import com.sack.rpgroll.tab.bossbar.BossBarEngine;
import com.sack.rpgroll.tab.bossbar.BossBarManager;
import com.sack.rpgroll.tab.event.BossBarCreateEvent;
import com.sack.rpgroll.tab.event.NametagUpdateEvent;
import com.sack.rpgroll.tab.event.ScoreboardChangeEvent;
import com.sack.rpgroll.tab.event.TabProfileChangeEvent;
import com.sack.rpgroll.tab.nametag.NametagEngine;
import com.sack.rpgroll.tab.nametag.NametagManager;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.scoreboard.ScoreboardEngine;
import com.sack.rpgroll.tab.scoreboard.ScoreboardManager;
import com.sack.rpgroll.tab.tablist.TabListEngine;
import com.sack.rpgroll.tab.visibility.WorldVisibilityService;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Único punto de entrada para "recalcular y aplicar todo lo que le
 * corresponde mostrar a un jugador" — lo llaman los listeners de ciclo de
 * vida (join/quit/world-change) y el comando admin, nunca un bucle por tick.
 * Dispara los eventos públicos correspondientes cuando algo realmente cambia.
 */
public class RefreshCoordinator {

    private final PlayerStateManager playerStateManager;
    private final TabListEngine tabListEngine;
    private final NametagManager nametagManager;
    private final NametagEngine nametagEngine;
    private final BelowNameManager belowNameManager;
    private final BelowNameEngine belowNameEngine;
    private final ScoreboardManager scoreboardManager;
    private final ScoreboardEngine scoreboardEngine;
    private final BossBarManager bossBarManager;
    private final BossBarEngine bossBarEngine;
    private final WorldVisibilityService worldVisibilityService;

    public RefreshCoordinator(PlayerStateManager playerStateManager, TabListEngine tabListEngine,
            NametagManager nametagManager, NametagEngine nametagEngine, BelowNameManager belowNameManager,
            BelowNameEngine belowNameEngine, ScoreboardManager scoreboardManager, ScoreboardEngine scoreboardEngine,
            BossBarManager bossBarManager, BossBarEngine bossBarEngine, WorldVisibilityService worldVisibilityService) {
        this.playerStateManager = playerStateManager;
        this.tabListEngine = tabListEngine;
        this.nametagManager = nametagManager;
        this.nametagEngine = nametagEngine;
        this.belowNameManager = belowNameManager;
        this.belowNameEngine = belowNameEngine;
        this.scoreboardManager = scoreboardManager;
        this.scoreboardEngine = scoreboardEngine;
        this.bossBarManager = bossBarManager;
        this.bossBarEngine = bossBarEngine;
        this.worldVisibilityService = worldVisibilityService;
    }

    /** Recalcula todo para UN jugador re-evaluando su contexto (join, cambio de mundo, comando admin). */
    public void refreshPlayer(Player player) {

        resolveAndFireEvent(player);
        applyActiveState(player);
    }

    /**
     * Aplica lo que YA está marcado como perfil activo del jugador, sin
     * volver a resolver el Context Engine — usado tras
     * {@link PlayerStateManager#forceProfile} (API pública / {@code /tabadmin profile})
     * para que el override explícito no se pise a sí mismo.
     */
    public void applyActiveState(Player player) {
        applyPerPlayer(player, playerStateManager.activeProfile(player).orElse(null));
        tabListEngine.apply(player);
    }

    /** Recalcula todo para TODOS los jugadores online — más caro, usar solo en reload/eventos globales. */
    public void refreshAll() {

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player player : online) {
            resolveAndFireEvent(player);
            applyPerPlayer(player, playerStateManager.activeProfile(player).orElse(null));
        }

        tabListEngine.refreshAll();
        worldVisibilityService.refreshAll();
    }

    /**
     * Pasada liviana llamada por un scheduler de baja frecuencia — SOLO
     * re-renderiza texto animado (header/footer) a partir del estado ya
     * resuelto en caché. No re-evalúa contexto ni recalcula sorting/teams
     * (eso sigue siendo 100% event-driven vía {@link #refreshPlayer}/{@link #refreshAll}).
     */
    public void tickAnimations() {
        tabListEngine.reapplyAnimatedElements();
    }

    public void clearPlayer(Player player) {

        tabListEngine.clear(player);
        nametagEngine.clear(player);
        scoreboardEngine.clear(player);
        bossBarEngine.hideAll(player);
        playerStateManager.clear(player);
    }

    private void resolveAndFireEvent(Player player) {

        TABProfile previous = playerStateManager.activeProfile(player).orElse(null);
        TABProfile resolved = playerStateManager.refresh(player);

        if (previous == null || !previous.id().equals(resolved.id())) {
            Bukkit.getPluginManager().callEvent(new TabProfileChangeEvent(player, previous, resolved));
        }
    }

    private void applyPerPlayer(Player player, TABProfile profile) {

        if (profile == null) {
            return;
        }

        if (profile.nametagId() != null) {
            nametagManager.get(profile.nametagId()).ifPresent(definition -> {
                nametagEngine.apply(player, definition);
                Bukkit.getPluginManager().callEvent(new NametagUpdateEvent(player, definition.id()));
            });
        }

        if (profile.belownameId() != null) {
            List<Player> onlineNow = new ArrayList<>(Bukkit.getOnlinePlayers());
            belowNameManager.get(profile.belownameId())
                    .ifPresent(definition -> belowNameEngine.apply(player, definition, onlineNow));
        }

        if (profile.scoreboardId() != null) {
            scoreboardManager.getResolved(profile.scoreboardId()).ifPresent(definition -> {
                scoreboardEngine.apply(player, definition);
                Bukkit.getPluginManager().callEvent(new ScoreboardChangeEvent(player, null, definition.id()));
            });
        }

        if (profile.bossbarId() != null) {
            bossBarManager.get(profile.bossbarId()).ifPresent(definition -> {
                bossBarEngine.show(player, definition);
                Bukkit.getPluginManager().callEvent(new BossBarCreateEvent(player, definition.id()));
            });
        }
    }

}
