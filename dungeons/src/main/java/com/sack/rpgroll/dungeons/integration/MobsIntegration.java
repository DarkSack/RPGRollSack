package com.sack.rpgroll.dungeons.integration;

import com.sack.rpgroll.mobs.MobsPlugin;
import com.sack.rpgroll.mobs.core.MobDefinition;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * Puente con RPGRoll-Mobs — a diferencia del resto de las integraciones
 * de este addon, esta es una dependencia dura (<code>depend</code> en
 * plugin.yml, no softdepend): sin Mobs instalado, Dungeons no tiene
 * forma de poblar salas de combate ni jefes.
 */
public final class MobsIntegration {

    private MobsIntegration() {
    }

    public static Optional<MobDefinition> resolveMob(String mobId) {
        return plugin().getMobManager().get(mobId);
    }

    public static Optional<LivingEntity> spawnMob(String mobId, Location location) {
        return resolveMob(mobId).flatMap(definition -> plugin().getEngine().spawnMob(definition, location));
    }

    private static MobsPlugin plugin() {

        var plugin = Bukkit.getPluginManager().getPlugin("RPGRoll-Mobs");

        if (!(plugin instanceof MobsPlugin mobsPlugin)) {
            throw new IllegalStateException("RPGRoll-Mobs no está disponible — es una dependencia dura de Dungeons.");
        }

        return mobsPlugin;
    }

}
