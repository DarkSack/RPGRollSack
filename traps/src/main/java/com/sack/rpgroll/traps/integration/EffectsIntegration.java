package com.sack.rpgroll.traps.integration;

import com.sack.rpgroll.effects.api.EffectsAPI;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/**
 * Integración blanda con RPGRoll-Effects para la acción EFFECT (veneno,
 * sangrado, quemadura...). Softdepend real: si el addon no está instalado
 * o todavía no terminó de arrancar, {@link #apply} devuelve false en vez
 * de crashear — mismo criterio que fishing/magic/seasons.
 */
public final class EffectsIntegration {

    private EffectsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Effects") && EffectsAPI.isReady();
    }

    public static boolean apply(String effectId, LivingEntity target) {

        if (!isAvailable()) {
            return false;
        }

        return EffectsAPI.get().apply(effectId, target);
    }

}
