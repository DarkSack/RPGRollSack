package com.sack.rpgroll.ranching.core.breeding;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Corre cada {@code pregnancy-check-interval-ticks}: descuenta la
 * gestación de cada hembra preñada y, al llegar a término, hace nacer su
 * camada ya congelada en {@link BreedingEngine#birthPending}.
 */
public class PregnancyTask extends BukkitRunnable {

    private final AnimalManager animalManager;
    private final BreedingEngine breedingEngine;
    private final long intervalTicks;

    public PregnancyTask(AnimalManager animalManager, BreedingEngine breedingEngine, long intervalTicks) {
        this.animalManager = animalManager;
        this.breedingEngine = breedingEngine;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void run() {

        for (Animal animal : List.copyOf(animalManager.getAll())) {

            if (!animal.isPregnant()) {
                continue;
            }

            Entity entity = Bukkit.getEntity(animal.id());

            if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) {
                continue;
            }

            boolean dueNow = animal.advancePregnancy(intervalTicks);

            if (dueNow) {
                breedingEngine.birthPending(animal, livingEntity.getLocation());
            }
        }
    }

}
