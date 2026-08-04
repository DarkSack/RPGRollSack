package com.sack.rpgroll.seasons.integration;

import com.sack.rpgroll.mobs.MobsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * Puente blando con RPGRoll-Mobs (softdepend) — a diferencia de la misma
 * clase en RPGRoll-Dungeons, acá SIEMPRE hay que chequear {@link
 * #isAvailable()} antes de usarla: sin Mobs instalado, Seasons sigue
 * funcionando (calendario/clima/vegetación), solo no hay spawns de
 * temporada ni jefes exclusivos.
 */
public final class MobsIntegration {

    private MobsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs") instanceof MobsPlugin;
    }

    public static Optional<LivingEntity> spawnMob(String mobId, Location location) {

        if (!isAvailable()) {
            return Optional.empty();
        }

        MobsPlugin plugin = (MobsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs");

        return plugin.getMobManager().get(mobId)
                .flatMap(definition -> plugin.getEngine().spawnMob(definition, location));
    }

}
