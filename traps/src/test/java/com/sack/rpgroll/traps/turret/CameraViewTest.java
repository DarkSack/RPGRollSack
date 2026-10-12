package com.sack.rpgroll.traps.turret;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CameraViewTest {

    @Test
    void originSurvivesBeingStoredInThePlayer() {
        CameraView.Origin origin = new CameraView.Origin("world_nether", -12.5, 64.0, 300.25, 90f, -15.5f,
                GameMode.ADVENTURE);

        assertEquals(origin, CameraView.Origin.decode(origin.encode()));
    }

    @Test
    void garbageIsNotAnOrigin() {
        assertNull(CameraView.Origin.decode(null));
        assertNull(CameraView.Origin.decode("world;1;2;3"));
        assertNull(CameraView.Origin.decode("world;x;64;0;0;0;SURVIVAL"));
        assertNull(CameraView.Origin.decode("world;1;64;0;0;0;FLYING"));
    }

}
