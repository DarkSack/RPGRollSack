package com.sack.rpgroll.ranching.core.genetics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneMutationTest {

    @ParameterizedTest
    @CsvSource({"-0.5,0", "0,0", "0.5,0.5", "1,1", "1.5,1"})
    void chanceIsClampedBetweenZeroAndOne(double input, double expected) {
        GeneMutation mutation = new GeneMutation("golden-wool", null, MutationEffectType.COSMETIC_TAG, 1.0, input);
        assertEquals(expected, mutation.chance());
    }

    @Test
    void effectTypeDefaultsToCosmeticTagWhenNull() {
        GeneMutation mutation = new GeneMutation("golden-wool", null, null, 1.0, 0.5);
        assertEquals(MutationEffectType.COSMETIC_TAG, mutation.effectType());
    }

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new GeneMutation(null, null, MutationEffectType.MULTIPLY, 2.0, 0.1));
    }

}
