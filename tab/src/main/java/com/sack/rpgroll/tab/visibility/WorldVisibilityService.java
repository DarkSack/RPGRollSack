package com.sack.rpgroll.tab.visibility;

import com.sack.rpgroll.tab.integration.ProtocolLibHook;
import com.sack.rpgroll.tab.profile.PlayerStateManager;
import com.sack.rpgroll.tab.profile.TABProfile;
import com.sack.rpgroll.tab.tablist.TablistDefinition;
import com.sack.rpgroll.tab.tablist.TablistManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * "Playerlist por mundo" (sección 19/21) a nivel de ENTRADAS del tablist,
 * no solo de header/footer: si el tablist activo de un jugador restringe
 * {@code worlds}, los jugadores fuera de esos mundos se ocultan de su lista
 * (y viceversa) usando {@link VisibilityEngine}. Requiere ProtocolLib — sin
 * él, todos los jugadores online siguen viéndose entre sí en el tablist
 * (el resto del addon — header/footer, sorting, teams — sigue funcionando).
 */
public class WorldVisibilityService {

    private final VisibilityEngine visibilityEngine;
    private final PlayerStateManager playerStateManager;
    private final TablistManager tablistManager;

    public WorldVisibilityService(VisibilityEngine visibilityEngine, PlayerStateManager playerStateManager,
            TablistManager tablistManager) {
        this.visibilityEngine = visibilityEngine;
        this.playerStateManager = playerStateManager;
        this.tablistManager = tablistManager;
    }

    /** Recalcula, para TODOS los pares de jugadores online, si deberían verse en el tablist del otro. */
    public void refreshAll() {

        if (!ProtocolLibHook.isPresent()) {
            return;
        }

        List<Player> online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player viewer : online) {
            for (Player subject : online) {

                if (viewer.equals(subject)) {
                    continue;
                }

                boolean shouldSee = shouldSee(viewer, subject);
                visibilityEngine.setListed(viewer, subject, shouldSee);
            }
        }
    }

    private boolean shouldSee(Player viewer, Player subject) {

        List<String> viewerWorlds = worldFilterOf(viewer);

        return viewerWorlds.isEmpty() || viewerWorlds.contains(subject.getWorld().getName());
    }

    private List<String> worldFilterOf(Player player) {

        TABProfile profile = playerStateManager.activeProfile(player).orElse(null);

        if (profile == null || profile.tablistId() == null) {
            return List.of();
        }

        TablistDefinition tablist = tablistManager.get(profile.tablistId()).orElse(null);
        return tablist == null ? List.of() : tablist.worldFilter();
    }

}
