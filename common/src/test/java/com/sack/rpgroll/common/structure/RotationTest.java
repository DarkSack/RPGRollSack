package com.sack.rpgroll.common.structure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RotationTest {

    @Test
    void cw90MapsCornersInsideTheRotatedBox() {
        // caja 3 de ancho (x) y 5 de largo (z): tras 90° es 5×3
        assertEquals(5, Rotation.CW_90.rotatedWidth(3, 5));
        assertEquals(3, Rotation.CW_90.rotatedLength(3, 5));
        assertArrayEquals(new int[] { 4, 0 }, Rotation.CW_90.apply(0, 0, 3, 5));
        assertArrayEquals(new int[] { 0, 2 }, Rotation.CW_90.apply(2, 4, 3, 5));
    }

    @Test
    void everyRotationStaysInsideItsBox() {
        for (Rotation r : Rotation.values()) {
            int rw = r.rotatedWidth(4, 7);
            int rl = r.rotatedLength(4, 7);
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 7; z++) {
                    int[] p = r.apply(x, z, 4, 7);
                    assertEquals(true, p[0] >= 0 && p[0] < rw && p[1] >= 0 && p[1] < rl, r + " " + x + "," + z);
                }
            }
        }
    }

    @Test
    void fourTurnsAreIdentity() {
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 7; z++) {
                int cx = x;
                int cz = z;
                int w = 4;
                int l = 7;
                for (int i = 0; i < 4; i++) {
                    int[] p = Rotation.CW_90.apply(cx, cz, w, l);
                    int nw = Rotation.CW_90.rotatedWidth(w, l);
                    l = Rotation.CW_90.rotatedLength(w, l);
                    w = nw;
                    cx = p[0];
                    cz = p[1];
                }
                assertEquals(x, cx);
                assertEquals(z, cz);
            }
        }
    }

    @Test
    void ccwUndoesCw() {
        int[] p = Rotation.CW_90.apply(1, 2, 4, 7);
        int[] q = Rotation.CCW_90.apply(p[0], p[1], Rotation.CW_90.rotatedWidth(4, 7), Rotation.CW_90.rotatedLength(4, 7));
        assertArrayEquals(new int[] { 1, 2 }, q);
    }

    @Test
    void nextCycles() {
        assertEquals(Rotation.NONE, Rotation.CCW_90.next());
        assertEquals(Rotation.CW_90, Rotation.NONE.next());
    }
}
