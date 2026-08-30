package com.sack.rpgroll.ranching.core.genetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllelePairTest {

    @Test
    void resolveDelegatesToTheGivenDominance() {
        AllelePair pair = new AllelePair(20, 80);

        assertEquals(80.0, pair.resolve(GeneDominance.DOMINANT));
        assertEquals(20.0, pair.resolve(GeneDominance.RECESSIVE));
        assertEquals(50.0, pair.resolve(GeneDominance.MIXED));
    }

}
