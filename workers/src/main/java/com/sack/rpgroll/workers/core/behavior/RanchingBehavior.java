package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.ranching.api.RanchingAPI;
import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * El ganadero busca el animal de RPGRoll-Ranching más cercano y lo
 * alimenta de verdad vía {@code RanchingAPI.feedAnimal(...)} — integración
 * real, no simulada, porque RanchingAPI no exige un {@code Player} para
 * estas acciones (a diferencia de FishingAPI, ver {@link FishingBehavior}).
 */
public class RanchingBehavior implements ProfessionBehavior {

    private final double searchRadius;
    private final String feedId;

    public RanchingBehavior(double searchRadius, String feedId) {
        this.searchRadius = searchRadius;
        this.feedId = feedId;
    }

    @Override
    public void work(Worker worker, LivingEntity entity, Profession profession) {

        if (!RanchingAPI.isReady()) {
            return;
        }

        Location origin = entity.getLocation();
        World world = origin.getWorld();

        if (world == null) {
            return;
        }

        Entity nearestAnimalEntity = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity nearby : world.getNearbyEntities(origin, searchRadius, searchRadius, searchRadius)) {

            if (RanchingAPI.get().getAnimal(nearby).isEmpty()) {
                continue;
            }

            double distance = nearby.getLocation().distanceSquared(origin);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestAnimalEntity = nearby;
            }
        }

        if (nearestAnimalEntity == null) {
            return;
        }

        if (origin.distance(nearestAnimalEntity.getLocation()) > 2.5) {
            MovementUtil.moveToward(entity, nearestAnimalEntity.getLocation());
            return;
        }

        Animal animal = RanchingAPI.get().getAnimal(nearestAnimalEntity).orElse(null);

        if (animal != null && feedId != null && !feedId.isBlank()) {
            RanchingAPI.get().feedAnimal(animal, feedId);
        }
    }

}
