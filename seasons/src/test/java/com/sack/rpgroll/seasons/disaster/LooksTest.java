package com.sack.rpgroll.seasons.disaster;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LooksTest {

    @Test
    void particleReadsCountColorAndSize() {
        Looks.ParticleLook flame = Looks.particle("FLAME*4");
        assertEquals(Particle.FLAME, flame.particle());
        assertEquals(4, flame.count());
        assertNull(flame.data());

        Looks.ParticleLook dust = Looks.particle("dust*8 #7CFC00 1.2");
        Particle.DustOptions options = (Particle.DustOptions) dust.data();
        assertEquals(Color.fromRGB(0x7CFC00), options.getColor());
        assertEquals(1.2f, options.getSize());
    }

    @Test
    void soundKeyWithVolumeAndPitch() {
        Looks.SoundLook custom = Looks.sound("sackito:viento 2 0.5");
        assertEquals("sackito:viento", custom.key());
        assertEquals(2f, custom.volume());
        assertEquals(0.5f, custom.pitch());
        assertEquals(Looks.SoundLook.NONE, Looks.sound(""));
    }

    @Test
    void oldColonFormatIsRejectedInsteadOfCrashingWhenPlayed() {
        assertThrows(IllegalArgumentException.class, () -> Looks.sound("EVENT_RAID_HORN:1:0.8"));
        assertThrows(IllegalArgumentException.class, () -> Looks.color("verde"));
        assertThrows(IllegalArgumentException.class, () -> Looks.particle("NOT_A_PARTICLE"));
    }

}
