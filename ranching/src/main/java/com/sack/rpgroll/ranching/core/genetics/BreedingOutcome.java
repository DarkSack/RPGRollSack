package com.sack.rpgroll.ranching.core.genetics;

import java.util.List;
import java.util.Map;

/** Resultado de concebir UNA cría — genotipo crudo, fenotipo ya resuelto, y mutaciones que se dispararon. */
public record BreedingOutcome(Map<String, AllelePair> genotype, Map<String, Double> phenotype,
        List<GeneMutation> triggeredMutations) {
}
