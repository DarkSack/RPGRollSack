package com.sack.rpgroll.ranching.core.genetics;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticsEngineTest {

    private Gene gene(GeneDominance dominance, List<GeneMutation> mutations) {
        return new Gene("milk", null, null, "milk_production", dominance, 0, 100, java.util.Set.of(), mutations);
    }

    @Test
    void createFounderGenotypeCreatesOneAllelePairPerGene() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene milk = gene(GeneDominance.MIXED, List.of());
        Gene fertility = new Gene("fertility", null, null, "fertility", GeneDominance.DOMINANT, 0, 100,
                java.util.Set.of(), List.of());

        Map<String, AllelePair> genotype = engine.createFounderGenotype(List.of(milk, fertility));

        assertEquals(2, genotype.size());
        assertTrue(genotype.containsKey("milk"));
        assertTrue(genotype.containsKey("fertility"));
    }

    @Test
    void founderGenotypeAllelesStayWithinGeneBounds() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene bounded = new Gene("milk", null, null, "milk_production", GeneDominance.MIXED, 20, 40, java.util.Set.of(),
                List.of());

        for (int i = 0; i < 200; i++) {
            AllelePair pair = engine.createFounderGenotype(List.of(bounded)).get("milk");
            assertTrue(pair.alleleA() >= 20 && pair.alleleA() <= 40);
            assertTrue(pair.alleleB() >= 20 && pair.alleleB() <= 40);
        }
    }

    @Test
    void simpleModeProducesHomozygousChildPair() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.SIMPLE, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of());

        Map<String, AllelePair> mother = Map.of("milk", new AllelePair(50, 50));
        Map<String, AllelePair> father = Map.of("milk", new AllelePair(50, 50));

        BreedingOutcome outcome = engine.breed(mother, father, List.of(gene));

        AllelePair childPair = outcome.genotype().get("milk");
        assertEquals(childPair.alleleA(), childPair.alleleB());
    }

    @Test
    void breedResultPhenotypeAlwaysStaysWithinGeneBounds() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of());

        Map<String, AllelePair> mother = Map.of("milk", new AllelePair(0, 100));
        Map<String, AllelePair> father = Map.of("milk", new AllelePair(0, 100));

        for (int i = 0; i < 500; i++) {
            BreedingOutcome outcome = engine.breed(mother, father, List.of(gene));
            double value = outcome.phenotype().get("milk");
            assertTrue(value >= 0 && value <= 100, "phenotype out of bounds: " + value);
        }
    }

    @Test
    void breedWithZeroMutationChanceNeverTriggersMutations() {
        GeneMutation mutation = new GeneMutation("golden", null, MutationEffectType.OVERRIDE, 100, 0);
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of(mutation));

        Map<String, AllelePair> mother = Map.of("milk", new AllelePair(50, 50));
        Map<String, AllelePair> father = Map.of("milk", new AllelePair(50, 50));

        for (int i = 0; i < 200; i++) {
            BreedingOutcome outcome = engine.breed(mother, father, List.of(gene));
            assertTrue(outcome.triggeredMutations().isEmpty());
        }
    }

    @Test
    void breedWithCertainMutationChanceAlwaysTriggersAndOverridesPhenotype() {
        GeneMutation mutation = new GeneMutation("giant", null, MutationEffectType.OVERRIDE, 99, 1.0);
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of(mutation));

        Map<String, AllelePair> mother = Map.of("milk", new AllelePair(10, 10));
        Map<String, AllelePair> father = Map.of("milk", new AllelePair(10, 10));

        BreedingOutcome outcome = engine.breed(mother, father, List.of(gene));

        assertEquals(1, outcome.triggeredMutations().size());
        assertEquals(99.0, outcome.phenotype().get("milk"));
    }

    @Test
    void breedFillsInMissingParentGenotypeWithRandomFounderAllelesInsteadsOfFailing() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.ADVANCED, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of());

        BreedingOutcome outcome = engine.breed(new HashMap<>(), new HashMap<>(), List.of(gene));

        assertTrue(outcome.phenotype().get("milk") >= 0 && outcome.phenotype().get("milk") <= 100);
    }

    @Test
    void previewPhenotypesReturnsMinLessThanOrEqualToAverageLessThanOrEqualToMax() {
        GeneticsEngine engine = new GeneticsEngine(GeneticsMode.PROBABILISTIC, 0.0);
        Gene gene = gene(GeneDominance.MIXED, List.of());

        Map<String, AllelePair> mother = Map.of("milk", new AllelePair(10, 90));
        Map<String, AllelePair> father = Map.of("milk", new AllelePair(10, 90));

        List<GenePreviewStats> stats = engine.previewPhenotypes(mother, father, List.of(gene), 300);

        assertEquals(1, stats.size());
        GenePreviewStats stat = stats.get(0);
        assertTrue(stat.min() <= stat.average());
        assertTrue(stat.average() <= stat.max());
    }

}
