package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Corre cada {@code health-check-interval-ticks}: hace avanzar (y, si
 * corresponde, curar por tiempo) la enfermedad activa de cada animal
 * enfermo, aplica sus penalizaciones periódicas, intenta contagiar a
 * animales sanos cercanos de la misma especie, y — solo si el bienestar
 * de un animal sano es malo — tira una chance baja de que enferme
 * espontáneamente (un rancho descuidado invita enfermedad).
 */
public class HealthTask extends BukkitRunnable {

    private static final double BASE_ONSET_CHANCE = 0.002;
    private static final double ONSET_WELFARE_THRESHOLD = 40;
    private static final int CONTAGION_RADIUS = 6;

    private final AnimalManager animalManager;
    private final DiseaseManager diseaseManager;
    private final long intervalTicks;
    private final double contagionRadiusOverride;
    private final Random random = new Random();

    public HealthTask(AnimalManager animalManager, DiseaseManager diseaseManager, long intervalTicks,
            double contagionRadiusOverride) {
        this.animalManager = animalManager;
        this.diseaseManager = diseaseManager;
        this.intervalTicks = intervalTicks;
        this.contagionRadiusOverride = contagionRadiusOverride > 0 ? contagionRadiusOverride : CONTAGION_RADIUS;
    }

    @Override
    public void run() {

        List<Animal> snapshot = List.copyOf(animalManager.getAll());

        for (Animal animal : snapshot) {

            if (animal.isSick()) {
                progressDisease(animal);
            } else {
                maybeOnset(animal);
            }
        }

        for (Animal animal : snapshot) {

            if (!animal.isSick()) {
                continue;
            }

            Entity entity = Bukkit.getEntity(animal.id());

            if (entity instanceof LivingEntity livingEntity && livingEntity.isValid()) {
                attemptContagion(animal, livingEntity);
            }
        }
    }

    private void progressDisease(Animal animal) {

        Disease disease = diseaseManager.get(animal.activeDiseaseId()).orElse(null);

        if (disease == null) {
            animal.cure();
            return;
        }

        animal.setHealth(animal.health() - disease.healthPenaltyPerCheck());
        animal.setHappiness(animal.happiness() - disease.happinessPenaltyPerCheck());
        animal.reduceDiseaseDuration(intervalTicks);

        if (animal.diseaseRemainingTicks() <= 0) {
            animal.cure();
        }
    }

    private void maybeOnset(Animal animal) {

        if (animal.happiness() >= ONSET_WELFARE_THRESHOLD && animal.health() >= ONSET_WELFARE_THRESHOLD) {
            return;
        }

        if (diseaseManager.count() == 0 || random.nextDouble() >= BASE_ONSET_CHANCE) {
            return;
        }

        List<Disease> candidates = List.copyOf(diseaseManager.getAll());
        Disease disease = candidates.get(random.nextInt(candidates.size()));

        // La vacuna no da inmunidad total: reduce el riesgo (riskMultiplierFor = 1 - risk-reduction).
        if (random.nextDouble() < animal.riskMultiplierFor(disease.id())) {
            animal.infect(disease.id(), disease.durationTicks());
        }
    }

    private void attemptContagion(Animal sick, LivingEntity sickEntity) {

        Disease disease = diseaseManager.get(sick.activeDiseaseId()).orElse(null);

        if (disease == null) {
            return;
        }

        for (Entity nearby : sickEntity.getNearbyEntities(contagionRadiusOverride, contagionRadiusOverride,
                contagionRadiusOverride)) {

            var otherOptional = animalManager.resolve(nearby);

            if (otherOptional.isEmpty()) {
                continue;
            }

            Animal other = otherOptional.get();

            // Vacunado no quiere decir inmune: su riesgo ya va en riskMultiplierFor, más abajo.
            if (other.isSick() || !other.speciesId().equals(sick.speciesId())) {
                continue;
            }

            double susceptibility = 1.0 + (100 - other.health()) / 100.0;
            double chance = disease.contagionChance() * other.riskMultiplierFor(disease.id()) * susceptibility;

            if (random.nextDouble() < chance) {
                other.infect(disease.id(), disease.durationTicks());
            }
        }
    }

}
