package com.sack.rpgroll.recipes.read;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrewingReaderTest {

    @Test
    void sacaLaClaveDelHolderDeMinecraft() {
        assertEquals("minecraft:swiftness",
                BrewingReader.keyOf("Reference{ResourceKey[minecraft:potion / minecraft:swiftness]=Potion[...]}"));
        assertEquals("minecraft:sugar", BrewingReader.keyOf("ResourceKey[minecraft:item / minecraft:sugar]"));
        assertNull(BrewingReader.keyOf("Direct{net.minecraft.world.item.Item@1234}"));
        assertNull(BrewingReader.keyOf(null));
    }

    @Test
    void laTablaFijaEstaCompleta() {
        assertTrue(FallbackMixes.POTIONS.size() >= 50);
        assertEquals(2, FallbackMixes.CONTAINERS.size());
        assertTrue(FallbackMixes.POTIONS.stream().allMatch(mix -> mix.from().startsWith("minecraft:")
                && mix.to().startsWith("minecraft:") && mix.ingredients().size() == 1));
    }
}
