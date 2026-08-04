package com.sack.rpgroll.ranching.core.genetics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * El corazón del addon: sortea el genotipo de un animal "fundador" (sin
 * padres), y concibe el genotipo de una cría a partir de sus dos padres —
 * con o sin dominancia real según {@link GeneticsMode}, más una chance de
 * mutación por gen. No sabe nada de {@code Animal} ni de Bukkit: opera
 * puramente sobre {@code Map<String gen, AllelePair>}, para poder
 * testearse y reusarse (ej. desde el Breeding Planner) sin un mundo real.
 */
public class GeneticsEngine {

    private final GeneticsMode mode;
    private final double globalMutationChance;
    private final Random random = new Random();

    public GeneticsEngine(GeneticsMode mode, double globalMutationChance) {
        this.mode = mode;
        this.globalMutationChance = globalMutationChance;
    }

    /** Genotipo inicial para un animal sin padres (comprado/spawneado como fundador de una línea). */
    public Map<String, AllelePair> createFounderGenotype(List<Gene> genes) {

        Map<String, AllelePair> genotype = new HashMap<>();

        for (Gene gene : genes) {
            genotype.put(gene.id(), new AllelePair(gene.randomStarting(random), gene.randomStarting(random)));
        }

        return genotype;
    }

    /** Concibe la cría de dos padres — el resultado real que se aplica al nacer. */
    public BreedingOutcome breed(Map<String, AllelePair> motherGenotype, Map<String, AllelePair> fatherGenotype,
            List<Gene> genes) {

        Map<String, AllelePair> childGenotype = new HashMap<>();
        Map<String, Double> phenotype = new HashMap<>();
        List<GeneMutation> triggered = new ArrayList<>();

        for (Gene gene : genes) {

            AllelePair motherPair = motherGenotype.getOrDefault(gene.id(),
                    new AllelePair(gene.randomStarting(random), gene.randomStarting(random)));
            AllelePair fatherPair = fatherGenotype.getOrDefault(gene.id(),
                    new AllelePair(gene.randomStarting(random), gene.randomStarting(random)));

            AllelePair childPair = inheritPair(motherPair, fatherPair, gene);
            double resolvedPhenotype = gene.clamp(childPair.resolve(gene.dominance()));

            double mutationChance = hasMutationOverride(gene) ? mutationChanceOf(gene) : globalMutationChance;

            if (!gene.mutations().isEmpty() && random.nextDouble() < mutationChance) {

                GeneMutation mutation = pickMutation(gene);
                triggered.add(mutation);

                resolvedPhenotype = switch (mutation.effectType()) {
                    case MULTIPLY -> gene.clamp(resolvedPhenotype * mutation.effectValue());
                    case OVERRIDE -> gene.clamp(mutation.effectValue());
                    case COSMETIC_TAG -> resolvedPhenotype;
                };
            }

            childGenotype.put(gene.id(), childPair);
            phenotype.put(gene.id(), resolvedPhenotype);
        }

        return new BreedingOutcome(childGenotype, phenotype, triggered);
    }

    /** Distribución Monte Carlo (sin mutaciones) del fenotipo resultante — para previsualizar antes de aparear. */
    public List<GenePreviewStats> previewPhenotypes(Map<String, AllelePair> motherGenotype,
            Map<String, AllelePair> fatherGenotype, List<Gene> genes, int trials) {

        List<GenePreviewStats> stats = new ArrayList<>();

        for (Gene gene : genes) {

            AllelePair motherPair = motherGenotype.getOrDefault(gene.id(),
                    new AllelePair(gene.randomStarting(random), gene.randomStarting(random)));
            AllelePair fatherPair = fatherGenotype.getOrDefault(gene.id(),
                    new AllelePair(gene.randomStarting(random), gene.randomStarting(random)));

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double sum = 0;

            for (int i = 0; i < trials; i++) {

                AllelePair childPair = inheritPair(motherPair, fatherPair, gene);
                double value = gene.clamp(childPair.resolve(gene.dominance()));

                min = Math.min(min, value);
                max = Math.max(max, value);
                sum += value;
            }

            stats.add(new GenePreviewStats(gene.id(), gene.displayName(), min, sum / trials, max));
        }

        return stats;
    }

    private AllelePair inheritPair(AllelePair motherPair, AllelePair fatherPair, Gene gene) {

        if (mode == GeneticsMode.SIMPLE) {

            double average = (motherPair.resolve(gene.dominance()) + fatherPair.resolve(gene.dominance())) / 2.0;
            double noise = (random.nextDouble() - 0.5) * 0.1 * (gene.maxValue() - gene.minValue());
            double value = gene.clamp(average + noise);

            return new AllelePair(value, value);
        }

        // ADVANCED / PROBABILISTIC: segregación mendeliana — un alelo al azar de cada padre.
        double fromMother = random.nextBoolean() ? motherPair.alleleA() : motherPair.alleleB();
        double fromFather = random.nextBoolean() ? fatherPair.alleleA() : fatherPair.alleleB();

        return new AllelePair(fromMother, fromFather);
    }

    private boolean hasMutationOverride(Gene gene) {
        return gene.mutations().stream().anyMatch(mutation -> mutation.chance() > 0);
    }

    private double mutationChanceOf(Gene gene) {
        return gene.mutations().stream().mapToDouble(GeneMutation::chance).filter(chance -> chance > 0).max()
                .orElse(globalMutationChance);
    }

    private GeneMutation pickMutation(Gene gene) {
        List<GeneMutation> mutations = gene.mutations();
        return mutations.get(random.nextInt(mutations.size()));
    }

}
