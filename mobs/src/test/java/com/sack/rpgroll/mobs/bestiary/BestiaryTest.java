package com.sack.rpgroll.mobs.bestiary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BestiaryTest {

    @Test
    void addsKillsPerRarity() {
        int[] counts = Bestiary.add(null, 0);
        counts = Bestiary.add(counts, 0);
        counts = Bestiary.add(counts, 3);
        assertArrayEquals(new int[] {2, 0, 0, 1}, counts);
        assertEquals(3, Bestiary.total(counts));
    }

    @Test
    void rpgrollKeysAreValidNamespacedKeys() {
        assertEquals("rpgroll.rey_goblin", Bestiary.rpgrollKey("Rey Goblin".replace(' ', '_')));
        assertEquals("rpgroll.boss_x_1", Bestiary.rpgrollKey("boss:X 1"));
    }
}
