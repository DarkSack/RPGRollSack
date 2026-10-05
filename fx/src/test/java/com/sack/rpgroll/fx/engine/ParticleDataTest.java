package com.sack.rpgroll.fx.engine;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleDataTest {

    @Test
    void readsHexWithAndWithoutHash() {
        assertEquals(List.of(Color.fromRGB(0xFF8800), Color.fromRGB(0x00FF00)),
                ParticleData.parseColors("#FF8800, 00FF00"));
    }

    @Test
    void threeNumbersAreOneRgbColor() {
        assertEquals(List.of(Color.fromRGB(255, 136, 0)), ParticleData.parseColors("255,136,0"));
    }

    @Test
    void readsBukkitColorNames() {
        assertEquals(List.of(Color.ORANGE), ParticleData.parseColors("orange"));
    }

    @Test
    void skipsWhatItCannotRead() {
        assertEquals(List.of(Color.RED), ParticleData.parseColors("nope,RED"));
        assertTrue(ParticleData.parseColors("").isEmpty());
    }

}
