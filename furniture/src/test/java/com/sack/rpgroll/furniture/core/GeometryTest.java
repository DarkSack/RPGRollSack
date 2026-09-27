package com.sack.rpgroll.furniture.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Giros de desplazamientos y direcciones: lo que decide dónde van barreras y asientos. */
class GeometryTest {

    @Test
    void offsetsTurnWithTheFurniture() {

        Offset right = new Offset(-1, 0, 0);       // con el mueble mirando al sur, un bloque al oeste

        assertArrayEquals(new int[] {-1, 0, 0}, right.rotateBlock(0));
        // Mirando al oeste (90): lo que estaba al oeste pasa al norte.
        assertArrayEquals(new int[] {0, 0, -1}, right.rotateBlock(90));
        assertArrayEquals(new int[] {1, 0, 0}, right.rotateBlock(180));
        assertArrayEquals(new int[] {0, 0, 1}, right.rotateBlock(270));

        // El frente (+Z con el mueble al sur) queda siempre hacia donde mira.
        Offset front = new Offset(0, 0, 1);
        assertArrayEquals(new int[] {-1, 0, 0}, front.rotateBlock(90));
        assertArrayEquals(new int[] {0, 0, -1}, front.rotateBlock(180));
    }

    @Test
    void diagonalFurnitureUsesTheNearestQuarterForBlocks() {
        assertArrayEquals(new Offset(-1, 0, 0).rotateBlock(90), new Offset(-1, 0, 0).rotateBlock(80));
        assertArrayEquals(new Offset(-1, 0, 0).rotateBlock(0), new Offset(-1, 0, 0).rotateBlock(44));
    }

    @Test
    void placementSnapsToItsDirections() {

        Placement four = new Placement(Set.of(Surface.FLOOR), 4, 0);
        assertEquals(90f, four.snap(100f));
        assertEquals(0f, four.snap(-30f));
        assertEquals(0f, four.snap(350f));

        Placement eight = new Placement(Set.of(Surface.FLOOR), 8, 0);
        assertEquals(45f, eight.snap(50f));
        assertEquals(315f, eight.snap(-40f));

        assertEquals(16, new Placement(Set.of(Surface.FLOOR), 99, 0).rotations());
    }

    @Test
    void offsetsParseWithCommasOrSpaces() {
        assertEquals(new Offset(1, 0.5, -2), Offset.parse("1,0.5,-2"));
        assertEquals(new Offset(1, 0.5, -2), Offset.parse(" 1 0.5 -2 "));
    }
}
