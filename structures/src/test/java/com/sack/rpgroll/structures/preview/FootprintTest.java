package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.storage.Box;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FootprintTest {

    @Test
    void footprintRotatedSwapsWidthAndLength() {
        Rotation r = Rotation.CW_90;
        Footprint f = Footprint.of(10, 5, 20, r.rotatedWidth(4, 2), 3, r.rotatedLength(4, 2));
        assertEquals(new Footprint(10, 5, 20, 11, 7, 23), f);
    }

    @Test
    void intersectsIsInclusive() {
        Footprint a = Footprint.of(0, 0, 0, 3, 3, 3);
        assertTrue(a.intersects(Footprint.of(2, 2, 2, 3, 3, 3)), "comparten un bloque");
        assertFalse(a.intersects(Footprint.of(3, 0, 0, 3, 3, 3)), "contiguas pero sin compartir");
        assertTrue(a.intersects(a));
    }

    @Test
    void ofBoxKeepsExtremes() {
        assertEquals(new Footprint(1, 2, 3, 4, 5, 6), Footprint.of(new Box(1, 2, 3, 4, 5, 6)));
    }

    @Test
    void edgesAreTheTwelveEdgesWithoutRepeats() {
        List<int[]> e = Footprint.of(0, 0, 0, 3, 3, 3).edges();
        // 8 esquinas + 12 aristas * 1 bloque intermedio
        assertEquals(20, e.size());
        assertEquals(e.size(), e.stream().map(Arrays::toString).distinct().count());
        assertEquals(1, Footprint.of(0, 0, 0, 1, 1, 1).edges().size());
    }
}
