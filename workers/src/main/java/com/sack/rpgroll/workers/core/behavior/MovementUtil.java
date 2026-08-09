package com.sack.rpgroll.workers.core.behavior;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/** Reusa el pathfinder A* de vanilla (vía {@link Mob#getPathfinder()}) — nunca se reimplementa navegación propia. */
public final class MovementUtil {

    private MovementUtil() {
    }

    public static void moveToward(LivingEntity entity, Location target) {

        if (target == null || !(entity instanceof Mob mob)) {
            return;
        }

        mob.getPathfinder().moveTo(target, 1.0);
    }

    public static void stop(LivingEntity entity) {

        if (entity instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
        }
    }

}
