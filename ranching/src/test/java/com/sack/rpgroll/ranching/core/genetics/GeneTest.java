package com.sack.rpgroll.ranching.core.genetics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneTest {

    private Gene gene(double min, double max) {
        return new Gene("milk", null, null, "milk_production", GeneDominance.MIXED, min, max, Set.of(), List.of());
    }

    @Test
    void minValueCannotBeNegative() {
        Gene gene = gene(-10, 50);
        assertEquals(0, gene.minValue());
    }

    @Test
    void maxValueCannotBeBelowMinValue() {
        Gene gene = gene(30, 10);
        assertEquals(30, gene.maxValue());
    }

    @ParameterizedTest
    @CsvSource({"-5,0", "0,0", "50,50", "100,100", "150,100"})
    void clampKeepsValueWithinMinAndMax(double input, double expected) {
        Gene gene = gene(0, 100);
        assertEquals(expected, gene.clamp(input));
    }

    @Test
    void randomStartingNeverExceedsGeneBounds() {
        Gene gene = gene(20, 40);
        Random random = new Random(42);

        for (int i = 0; i < 1000; i++) {
            double value = gene.randomStarting(random);
            assertTrue(value >= 20 && value <= 40);
        }
    }

    @Test
    void appliesToIsUnrestrictedWhenApplicableSpeciesIsEmpty() {
        Gene gene = new Gene("milk", null, null, "milk_production", GeneDominance.MIXED, 0, 100, Set.of(), List.of());
        assertTrue(gene.appliesTo("cow"));
        assertTrue(gene.appliesTo("anything"));
    }

    @Test
    void appliesToIsRestrictedWhenApplicableSpeciesIsNotEmpty() {
        Gene gene = new Gene("milk", null, null, "milk_production", GeneDominance.MIXED, 0, 100, Set.of("cow"),
                List.of());
        assertTrue(gene.appliesTo("cow"));
        assertTrue(!gene.appliesTo("sheep"));
    }

    @Test
    void dominanceDefaultsToMixedWhenNull() {
        Gene gene = new Gene("milk", null, null, "milk_production", null, 0, 100, Set.of(), List.of());
        assertEquals(GeneDominance.MIXED, gene.dominance());
    }

    @Test
    void constructorRejectsNullAttributeKey() {
        assertThrows(NullPointerException.class,
                () -> new Gene("milk", null, null, null, GeneDominance.MIXED, 0, 100, Set.of(), List.of()));
    }

}
