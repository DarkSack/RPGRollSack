package com.sack.rpgroll.traps.turret;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraViewTest {

    @Test
    void originSurvivesBeingStoredInThePlayer() {
        CameraView.Origin origin = new CameraView.Origin("world_nether", -12.5, 64.0, 300.25, 90f, -15.5f,
                GameMode.ADVENTURE, 0.3f);

        assertEquals(origin, CameraView.Origin.decode(origin.encode()));
    }

    @Test
    void anOriginSavedBeforeFlySpeedGetsTheDefault() {
        assertEquals(new CameraView.Origin("world", 1, 64, 2, 0f, 0f, GameMode.SURVIVAL, 0.1f),
                CameraView.Origin.decode("world;1;64;2;0;0;SURVIVAL"));
    }

    @Test
    void movingWhileWatchingOnlyTurnsTheView() {
        Location post = new Location(null, 10.5, 63.88, -4.5, 0f, 0f);
        Location wanted = new Location(null, 12, 70, -1, 135f, -30f);

        Location kept = CameraView.lookOnly(post, wanted);

        assertEquals(10.5, kept.getX());
        assertEquals(63.88, kept.getY());
        assertEquals(-4.5, kept.getZ());
        assertEquals(135f, kept.getYaw());
        assertEquals(-30f, kept.getPitch());
    }

    /** Retener al que mira (o frenarle la vista en el borde) es un teletransporte que no lo mueve: no lo saca. */
    @Test
    void onlyATeleportThatMovesYouTakesYouOutOfTheCamera() {
        Location post = new Location(null, 10.5, 63.88, -4.5, 0f, 0f);

        assertTrue(CameraView.staysPut(post, new Location(null, 10.5, 63.88, -4.5, 120f, 40f)));
        assertFalse(CameraView.staysPut(post, new Location(null, 10.5, 70, -4.5, 0f, 0f)));
    }

    /** Una cámara o un monitor no son torretas: sus avisos tienen texto propio. */
    @Test
    void camerasAndMonitorsAreNotCalledTurrets() {
        assertEquals("camera.placed_by_item", CameraView.textKey("CAMERA", "placed_by_item"));
        assertEquals("camera.monitor_placed_by_item", CameraView.textKey("MONITOR", "placed_by_item"));
        assertEquals("admin.turret.placed_by_item", CameraView.textKey("ARROW_PROJECTILE", "placed_by_item"));
        assertEquals("admin.turret.not_owner", CameraView.textKey(null, "not_owner"));
    }

    @Test
    void aPlacedBlockFacesWhoPlacedIt() {
        // Quien mira al sur (yaw 0) pone el bloque delante: su frente mira al norte, hacia él.
        assertEquals(180f, CameraView.facing(0f));
        assertEquals(270f, CameraView.facing(90f));
        assertEquals(0f, CameraView.facing(-170f));
        assertEquals(90f, CameraView.facing(-95f));
    }

    @Test
    void namesLoseFormattingAndExcessLength() {
        assertEquals("Puerta norte", CameraView.cleanName("  &c&lPuerta <norte>{}  "));
        assertEquals(24, CameraView.cleanName("x".repeat(60)).length());
        assertNull(CameraView.cleanName(" &§ "));
        assertNull(CameraView.cleanName(null));
    }

    @Test
    void garbageIsNotAnOrigin() {
        assertNull(CameraView.Origin.decode(null));
        assertNull(CameraView.Origin.decode("world;1;2;3"));
        assertNull(CameraView.Origin.decode("world;x;64;0;0;0;SURVIVAL"));
        assertNull(CameraView.Origin.decode("world;1;64;0;0;0;FLYING"));
    }

}
