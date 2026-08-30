package com.sack.rpgroll.ranching.core.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneDominanceTest {

    @Test
    void dominantResolvesToTheHigherAllele() {
        assertEquals(80.0, GeneDominance.DOMINANT.resolve(80, 30));
        assertEquals(80.0, GeneDominance.DOMINANT.resolve(30, 80));
    }

    @Test
    void recessiveResolvesToTheLowerAllele() {
        assertEquals(30.0, GeneDominance.RECESSIVE.resolve(80, 30));
        assertEquals(30.0, GeneDominance.RECESSIVE.resolve(30, 80));
    }

    @Test
    void mixedResolvesToTheAverage() {
        assertEquals(55.0, GeneDominance.MIXED.resolve(80, 30));
    }

    @Test
    void resolveWithEqualAllelesReturnsThatValueForEveryMode() {
        assertEquals(50.0, GeneDominance.DOMINANT.resolve(50, 50));
        assertEquals(50.0, GeneDominance.RECESSIVE.resolve(50, 50));
        assertEquals(50.0, GeneDominance.MIXED.resolve(50, 50));
    }

}
