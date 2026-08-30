package com.sack.rpgroll.api.playerclass;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerClassTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new PlayerClass(null, "Warrior", null, null, null, null, null));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerClass("", "Warrior", null, null, null, null, null));
    }

    @Test
    void constructorRejectsNullDisplayName() {
        assertThrows(NullPointerException.class,
                () -> new PlayerClass("warrior", null, null, null, null, null, null));
    }

    @Test
    void constructorDefaultsNullFieldsToEmpty() {
        PlayerClass playerClass = new PlayerClass("warrior", "Warrior", null, null, null, null, null);

        assertEquals("", playerClass.description());
        assertEquals("", playerClass.icon());
        assertTrue(playerClass.baseAttributes().isEmpty());
        assertTrue(playerClass.passiveTraits().isEmpty());
        assertTrue(playerClass.lore().isEmpty());
    }

    @Test
    void constructorCopiesListsDefensively() {
        var traits = new java.util.ArrayList<>(List.of("shield-bash"));

        PlayerClass playerClass = new PlayerClass("warrior", "Warrior", "desc", null, traits, "icon", List.of());
        traits.add("charge");

        assertEquals(1, playerClass.passiveTraits().size());
    }
}
