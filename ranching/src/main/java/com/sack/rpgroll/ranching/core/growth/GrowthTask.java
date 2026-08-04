package com.sack.rpgroll.ranching.core.growth;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.animal.AnimalQuality;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Corre cada {@code growth-check-interval-ticks}: envejece cada animal,
 * decide su etapa de vida (por acumulado de edad contra la duración
 * configurada por especie), acerca su peso al peso objetivo de su etapa
 * actual, y recalcula su {@link AnimalQuality} a partir de sus genes
 * expresados y su felicidad acumulada.
 */
public class GrowthTask extends BukkitRunnable {

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final long intervalTicks;

    public GrowthTask(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            long intervalTicks) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void run() {

        for (Animal animal : List.copyOf(animalManager.getAll())) {

            Species species = speciesManager.get(animal.speciesId()).orElse(null);

            if (species == null) {
                continue;
            }

            Breed breed = animal.breedId() != null ? breedManager.get(animal.breedId()).orElse(null) : null;

            animal.addAge(intervalTicks);

            GrowthStage previousStage = animal.stage();
            updateStage(animal, species);
            updateWeight(animal, species, breed);
            updateQuality(animal);
            updateFertility(animal, species, breed);

            if (animal.stage() != previousStage) {
                syncAgeableModel(animal);
            }
        }
    }

    private void updateStage(Animal animal, Species species) {

        long babyEnd = species.babyStageDurationTicks();
        long juvenileEnd = babyEnd + species.juvenileStageDurationTicks();
        long elderStart = juvenileEnd + species.elderThresholdTicks();

        GrowthStage newStage;

        if (animal.ageTicks() < babyEnd) {
            newStage = GrowthStage.BABY;
        } else if (animal.ageTicks() < juvenileEnd) {
            newStage = GrowthStage.JUVENILE;
        } else if (species.elderThresholdTicks() <= 0 || animal.ageTicks() < elderStart) {
            newStage = GrowthStage.ADULT;
        } else {
            newStage = GrowthStage.ELDER;
        }

        animal.setStage(newStage);
    }

    private void updateWeight(Animal animal, Species species, Breed breed) {

        long fullGrowthTicks = species.babyStageDurationTicks() + species.juvenileStageDurationTicks();
        double progress = fullGrowthTicks <= 0 ? 1.0
                : Math.max(0, Math.min(1.0, (double) animal.ageTicks() / fullGrowthTicks));

        double target = species.baseWeightMin() + (species.baseWeightMax() - species.baseWeightMin()) * progress;
        target *= breed != null ? breed.weightMultiplier() : 1.0;

        if (animal.stage() == GrowthStage.ELDER) {
            target *= 0.95;
        }

        animal.setWeight(animal.weight() + (target - animal.weight()) * 0.1);
    }

    private void updateQuality(Animal animal) {

        if (animal.phenotype().isEmpty()) {
            return;
        }

        double averageGenePercent = animal.phenotype().values().stream().mapToDouble(Double::doubleValue).average()
                .orElse(50);

        double score = averageGenePercent * 0.7 + animal.happiness() * 0.3;
        animal.setQuality(AnimalQuality.fromScore(score));
    }

    private void updateFertility(Animal animal, Species species, Breed breed) {

        double stageFactor = switch (animal.stage()) {
            case ADULT -> 1.0;
            case ELDER -> 0.4;
            default -> 0.0;
        };

        double happinessFactor = 0.5 + (animal.happiness() / 100.0) * 0.5;
        double healthFactor = 0.5 + (animal.health() / 100.0) * 0.5;

        double fertility = species.baseFertility() * (breed != null ? breed.fertilityMultiplier() : 1.0)
                * stageFactor * happinessFactor * healthFactor;

        animal.setFertility(fertility);
    }

    private void syncAgeableModel(Animal animal) {

        Entity entity = Bukkit.getEntity(animal.id());

        if (entity instanceof Ageable ageable) {

            if (animal.stage() == GrowthStage.BABY || animal.stage() == GrowthStage.JUVENILE) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
    }

}
