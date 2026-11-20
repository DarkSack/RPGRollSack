package com.sack.rpgroll.traps.turret;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraMountTest {

    @Test
    void hangsFromAWallOrTheCeilingButNotFromTheFloor() {
        assertEquals(CameraMount.WALL, CameraMount.fromFace(BlockFace.NORTH));
        assertEquals(CameraMount.WALL, CameraMount.fromFace(BlockFace.EAST));
        assertEquals(CameraMount.CEILING, CameraMount.fromFace(BlockFace.DOWN));
        assertNull(CameraMount.fromFace(BlockFace.UP));
        assertNull(CameraMount.fromFace(null));
    }

    /** La cara es hacia dónde quedó el bloque respecto al muro: la cámara mira hacia ese lado. */
    @Test
    void onAWallItLooksAwayFromTheWall() {
        assertEquals(0f, CameraMount.yawAwayFrom(BlockFace.SOUTH));
        assertEquals(90f, CameraMount.yawAwayFrom(BlockFace.WEST));
        assertEquals(180f, CameraMount.yawAwayFrom(BlockFace.NORTH));
        assertEquals(270f, CameraMount.yawAwayFrom(BlockFace.EAST));
    }

    @Test
    void theViewStaysInsideTheArc() {
        // Pared mirando al norte (180): 80° a cada lado, de -30 (algo arriba) a 75 (abajo).
        assertArrayEquals(new float[] {150f, 10f}, CameraMount.WALL.clamp(180f, 150f, 10f));
        assertArrayEquals(new float[] {260f, 75f}, CameraMount.WALL.clamp(180f, 300f, 90f));
        assertArrayEquals(new float[] {100f, -30f}, CameraMount.WALL.clamp(180f, 0f, -90f));
        // Cruzar el 0/360 no confunde: 20 está a 40° de -20 (340).
        assertArrayEquals(new float[] {20f, 0f}, CameraMount.WALL.clamp(-20f, 20f, 0f));
        // Techo: 120° a cada lado y nunca mira hacia arriba.
        assertArrayEquals(new float[] {240f, 0f}, CameraMount.CEILING.clamp(0f, 200f, -30f));
    }

    @Test
    void whatIsBehindTheWallIsNotSeen() {
        assertTrue(CameraMount.WALL.covers(180f, 200f, 30f));
        assertFalse(CameraMount.WALL.covers(180f, 0f, 30f));
        assertFalse(CameraMount.WALL.covers(180f, 180f, -60f));
    }

    @Test
    void sweepingTurnsBackAtTheEdges() {
        CameraMount.Sweep step = CameraMount.WALL.sweep(new CameraMount.Sweep(78f, 1), 1.5f);
        assertEquals(new CameraMount.Sweep(79.5f, 1), step);

        step = CameraMount.WALL.sweep(step, 1.5f);
        assertEquals(new CameraMount.Sweep(80f, -1), step);

        assertEquals(new CameraMount.Sweep(-80f, 1), CameraMount.WALL.sweep(new CameraMount.Sweep(-79f, -1), 1.5f));
    }

    @Test
    void readsWhatWasSaved() {
        assertEquals(CameraMount.CEILING, CameraMount.of("CEILING"));
        assertNull(CameraMount.of(null));
        assertNull(CameraMount.of("SUELO"));
    }
}
