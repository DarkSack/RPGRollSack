package com.sack.rpgroll.extras.reinforce;

import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReinforcementPackTest {

    private static Block at(int x, int y, int z) {
        Block block = mock(Block.class);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        return block;
    }

    /** Cada casilla de un chunk, de y=-64 a 319, tiene su propia clave (también con x/z negativos). */
    @Test
    void everyCellOfAChunkGetsItsOwnKey() {
        Set<Integer> keys = new HashSet<>();
        for (int x = -16; x < 0; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 320; y += 37) {
                    int key = Reinforcement.pack(at(x, y, z));
                    keys.add(key);
                    assertEquals(true, key >= 0, "las claves van en el nombre de un NamespacedKey: sin signo");
                }
            }
        }
        assertEquals(16 * 16 * 11, keys.size());
    }
}
