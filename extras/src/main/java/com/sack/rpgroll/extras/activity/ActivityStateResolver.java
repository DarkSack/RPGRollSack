package com.sack.rpgroll.extras.activity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detecta el "estado de actividad" del jugador (sección 4: resting/walking/
 * sprinting/combat) sin recalcular nada por tick — el listener de
 * movimiento solo actualiza una marca de tiempo (barato, y solo dispara
 * cuando el jugador realmente se mueve), y {@link #resolve(Player)} decide
 * el estado bajo demanda cuando algo lo consulta (el propio tick de
 * regeneración de un stat, no antes).
 */
public class ActivityStateResolver implements Listener {

    private static final long COMBAT_WINDOW_MILLIS = 10_000;
    private static final long RESTING_THRESHOLD_MILLIS = 3_000;

    private final Map<UUID, Long> lastCombatMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMoveMillis = new ConcurrentHashMap<>();

    public ActivityState resolve(Player player) {

        if (isInCombat(player)) {
            return ActivityState.COMBAT;
        }

        if (player.isSprinting()) {
            return ActivityState.SPRINTING;
        }

        Long lastMove = lastMoveMillis.get(player.getUniqueId());
        boolean resting = lastMove == null || (System.currentTimeMillis() - lastMove) >= RESTING_THRESHOLD_MILLIS;

        return resting ? ActivityState.RESTING : ActivityState.WALKING;
    }

    public void markCombat(Player player) {
        lastCombatMillis.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isInCombat(Player player) {
        Long last = lastCombatMillis.get(player.getUniqueId());
        return last != null && (System.currentTimeMillis() - last) < COMBAT_WINDOW_MILLIS;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (movedToNewBlock(event)) {
            lastMoveMillis.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    private boolean movedToNewBlock(PlayerMoveEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return false;
        }

        return from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
    }

    public void clear(Player player) {
        lastCombatMillis.remove(player.getUniqueId());
        lastMoveMillis.remove(player.getUniqueId());
    }

}
