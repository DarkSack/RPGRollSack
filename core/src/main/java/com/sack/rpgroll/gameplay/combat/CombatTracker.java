package com.sack.rpgroll.gameplay.combat;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Registra en memoria el último momento en que cada jugador estuvo
 * involucrado en combate (golpeó o recibió daño). Usado para decidir si la
 * regeneración pasiva de salud/maná debe pausarse mientras el jugador está
 * en combate (ver {@code combat.natural_regen_in_combat} en gameplay.yml).
 */
public class CombatTracker {

    private final Map<UUID, Long> lastCombatAt = new ConcurrentHashMap<>();

    /**
     * Marca al jugador como recién involucrado en combate.
     */
    public void markInCombat(UUID uuid) {
        lastCombatAt.put(uuid, System.currentTimeMillis());
    }

    /**
     * @param combatDurationSeconds ventana de tiempo tras el último golpe
     *                              durante la cual se considera "en combate"
     */
    public boolean isInCombat(UUID uuid, int combatDurationSeconds) {
        Long last = lastCombatAt.get(uuid);
        if (last == null) {
            return false;
        }
        long elapsedMillis = System.currentTimeMillis() - last;
        return elapsedMillis < combatDurationSeconds * 1000L;
    }

    public void clear(UUID uuid) {
        lastCombatAt.remove(uuid);
    }

}
