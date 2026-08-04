package com.sack.rpgroll.ranching.core.production;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Species;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resuelve UNA unidad de producción (una ordeñada, una esquilada, un
 * huevo, la carne/cuero al morir) — cantidad y calidad dependen de
 * genética, raza, etapa de vida, bienestar, clima y enfermedad activa,
 * tal como pide el diseño original ("Cada recurso depende de: Genética,
 * Alimentación, Edad, Felicidad, Estación, Clima").
 * <p>
 * La genética influye buscando, entre los genes de la especie, alguno
 * cuyo {@code attribute-key} contenga el nombre del producto (ej. un gen
 * "milk_production" para el producto "milk") — si ninguno aplica, la
 * genética simplemente no influye en ese producto (factor neutro 1.0).
 */
public class ProductionEngine {

    public Optional<ProductionResult> produce(Animal animal, Species species, Breed breed,
            List<Gene> genesForSpecies, String productType, Double ambientTemperature, Disease activeDisease) {

        if (animal.isPregnant()) {
            return Optional.empty();
        }

        if (animal.stage() == GrowthStage.BABY || animal.stage() == GrowthStage.JUVENILE) {
            return Optional.empty();
        }

        Double baseAmount = species.baseProduction().get(productType.toLowerCase(Locale.ROOT));

        if (baseAmount == null) {
            return Optional.empty();
        }

        double geneticFactor = resolveGeneticFactor(animal, genesForSpecies, productType);
        double stageFactor = animal.stage() == GrowthStage.ELDER ? 0.7 : 1.0;
        double happinessFactor = 0.5 + (animal.happiness() / 100.0) * 0.5;
        double healthFactor = 0.5 + (animal.health() / 100.0) * 0.5;
        double climateFactor = ambientTemperature != null && (ambientTemperature < -10 || ambientTemperature > 35)
                ? 0.85
                : 1.0;
        double breedFactor = breed != null ? breed.productionMultiplier() : 1.0;
        double feedBonusFactor = 1.0 + animal.consumeProductionBonus() / 100.0;
        double diseaseFactor = activeDisease != null ? (1.0 - activeDisease.productionPenalty()) : 1.0;

        double amount = baseAmount * geneticFactor * stageFactor * happinessFactor * healthFactor * climateFactor
                * breedFactor * feedBonusFactor * diseaseFactor;

        double qualityScore = (geneticFactor * 100) * 0.5 + animal.happiness() * 0.3 + animal.health() * 0.2;
        ProductQuality quality = ProductQuality.fromScore(qualityScore);

        return Optional.of(new ProductionResult(productType, Math.max(0, amount), quality));
    }

    private double resolveGeneticFactor(Animal animal, List<Gene> genesForSpecies, String productType) {

        String needle = productType.toLowerCase(Locale.ROOT);

        List<Gene> matching = genesForSpecies.stream()
                .filter(gene -> gene.attributeKey().toLowerCase(Locale.ROOT).contains(needle)).toList();

        if (matching.isEmpty()) {
            return 1.0;
        }

        double sum = 0;

        for (Gene gene : matching) {

            double value = animal.phenotypeValue(gene.id(), (gene.minValue() + gene.maxValue()) / 2.0);
            double range = gene.maxValue() - gene.minValue();
            double normalized = range <= 0 ? 1.0 : (value - gene.minValue()) / range;

            sum += normalized;
        }

        return sum / matching.size();
    }

}
