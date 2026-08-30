package com.sack.rpgroll.traps.core;

import org.bukkit.Material;

/**
 * Configuración opcional de "bloque protegido" para una trampa (puertas
 * reforzadas, muros con llave). {@code requiredItemId} se resuelve primero
 * contra RPGRoll-Items (si está presente) y si no contra un Material vanilla
 * — mismo criterio softdepend que {@code ItemsIntegration} del resto del repo.
 */
public record TrapBlockConfig(
        Material material,
        boolean breakable,
        String requiredItemId,
        int breakTimeSeconds,
        boolean explosionImmune,
        boolean pistonImmune) {

    public static TrapBlockConfig none() {
        return new TrapBlockConfig(null, true, null, 0, false, false);
    }

    public boolean isConfigured() {
        return material != null;
    }

    public boolean requiresKey() {
        return requiredItemId != null && !requiredItemId.isBlank();
    }

}
