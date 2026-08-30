package com.sack.rpgroll.traps.integration;

import com.sack.rpgroll.mobs.MobsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

/**
 * Integración blanda con RPGRoll-Mobs para que una torreta reconozca mobs
 * custom como objetivo hostil. Simplificación deliberada: cualquier mob
 * custom (tiene {@code MobInstanceService.isRpgMob}) cuenta como hostil —
 * no se inspeccionan sus {@code goals()}/AI real, porque no existe en el
 * repo ningún flag centralizado "es hostil" que distinga eso. Softdepend
 * real — nunca crashea si RPGRoll-Mobs no está instalado.
 */
public final class MobsIntegration {

    private MobsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Mobs")
                && Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs") instanceof MobsPlugin;
    }

    public static boolean isCustomHostile(LivingEntity entity) {

        if (!isAvailable()) {
            return false;
        }

        var plugin = (MobsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs");
        return plugin.getInstanceService().isRpgMob(entity);
    }

}
