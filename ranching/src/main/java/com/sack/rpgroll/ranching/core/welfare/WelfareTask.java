package com.sack.rpgroll.ranching.core.welfare;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.integration.SeasonsIntegration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Corre cada {@code welfare-check-interval-ticks}: acerca la felicidad de
 * cada animal a su "objetivo" ambiental (en vez de saltar de golpe), y una
 * felicidad muy baja sostenida empieza a drenar salud lentamente — un
 * animal descuidado se enferma con el tiempo, no muere de golpe (no hay
 * muerte automática por negligencia en esta pasada: la salud tiene un piso
 * bajo pero nunca llega a 0 sola, para no matar animales de un jugador sin
 * una advertencia explícita primero).
 */
public class WelfareTask extends BukkitRunnable {

    private static final double MIN_HEALTH_FLOOR = 5;
    private static final int NEARBY_SCAN_RADIUS = 8;

    private final AnimalManager animalManager;
    private final WelfareEngine welfareEngine = new WelfareEngine();
    private final long intervalTicks;

    public WelfareTask(AnimalManager animalManager, long intervalTicks) {
        this.animalManager = animalManager;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void run() {

        for (Animal animal : List.copyOf(animalManager.getAll())) {

            Entity entity = Bukkit.getEntity(animal.id());

            if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) {
                continue;
            }

            Location location = livingEntity.getLocation();
            int nearby = countNearbySameSpecies(livingEntity, animal.speciesId());
            Double temperature = SeasonsIntegration.temperature(location);

            double target = welfareEngine.computeTargetHappiness(location, nearby, animal.health(), temperature);
            double newHappiness = animal.happiness() + (target - animal.happiness()) * 0.2;
            animal.setHappiness(newHappiness);

            if (animal.happiness() < 30) {
                animal.setHealth(Math.max(MIN_HEALTH_FLOOR, animal.health() - 1));
            } else if (animal.happiness() > 70 && animal.health() < 100) {
                animal.setHealth(Math.min(100, animal.health() + 0.5));
            }

            animal.tickImmunities(intervalTicks);
        }
    }

    private int countNearbySameSpecies(LivingEntity source, String speciesId) {

        int count = 0;

        for (Entity nearby : source.getNearbyEntities(NEARBY_SCAN_RADIUS, NEARBY_SCAN_RADIUS, NEARBY_SCAN_RADIUS)) {

            if (!animalManager.isTracked(nearby)) {
                continue;
            }

            var other = animalManager.resolve(nearby);

            if (other.isPresent() && other.get().speciesId().equals(speciesId)) {
                count++;
            }
        }

        return count;
    }

}
