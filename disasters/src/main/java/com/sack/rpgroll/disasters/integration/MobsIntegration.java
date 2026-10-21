package com.sack.rpgroll.disasters.integration;

import com.sack.rpgroll.mobs.MobsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/** Puente blando con RPGRoll-Mobs (softdepend): sin él, los mobs {@code rpgroll:<id>} no salen. */
public final class MobsIntegration {

    private MobsIntegration() {
    }

    /** Primero se mira si el plugin está: sin él, tocar {@link MobsPlugin} lanzaría NoClassDefFoundError. */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Mobs")
                && Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs") instanceof MobsPlugin;
    }

    public static Optional<LivingEntity> spawnMob(String mobId, Location location) {

        if (!isAvailable()) {
            return Optional.empty();
        }

        MobsPlugin plugin = (MobsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs");
        return plugin.getMobManager().get(mobId).flatMap(definition -> plugin.getEngine().spawnMob(definition, location));
    }

}
