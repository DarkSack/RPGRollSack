package com.sack.rpgroll.items.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDefinitionTest {

    private ItemDefinition minimal(Material material, String id) {
        return new ItemDefinition(id, null, material, null, null, null, null, null, null, false, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0, null);
    }

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class, () -> minimal(Material.STICK, null));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> minimal(Material.STICK, "  "));
    }

    @Test
    void constructorRejectsNullMaterial() {
        assertThrows(NullPointerException.class,
                () -> new ItemDefinition("sword", null, null, null, null, null, null, null, null, false, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0,
                        null));
    }

    @Test
    void constructorAppliesDefaultsForOptionalFields() {
        ItemDefinition definition = minimal(Material.STICK, "sword");

        assertEquals("misc", definition.pack());
        assertEquals("sword", definition.displayName());
        assertEquals("common", definition.rarityId());
        assertTrue(definition.lore().isEmpty());
        assertTrue(definition.flags().isEmpty());
        assertTrue(definition.stats().isEmpty());
        assertTrue(definition.upgrades().isEmpty());
        assertEquals(ItemRequirements.none(), definition.requirements());
        assertEquals(0, definition.durability().maxDurability());
    }

    @Test
    void statReturnsZeroForMissingKey() {
        ItemDefinition definition = minimal(Material.STICK, "sword");

        assertEquals(0.0, definition.stat("damage"));
    }

    @Test
    void statReturnsConfiguredValue() {
        ItemDefinition definition = new ItemDefinition("sword", null, Material.STICK, null, null, null, null, null,
                null, false, null, null, null, Map.of("damage", 7.5), null, null, null, null, null, null, null, null,
                null, null, null, null, 0, 0, null);

        assertEquals(7.5, definition.stat("damage"));
    }

    @Test
    void actionsForReturnsEmptyListWhenTriggerNotConfigured() {
        ItemDefinition definition = minimal(Material.STICK, "sword");

        assertTrue(definition.actionsFor(ItemTrigger.RIGHT_CLICK).isEmpty());
    }

    @Test
    void actionsForReturnsConfiguredActionsForMatchingTrigger() {
        ItemAction action = new ItemAction("MESSAGE", Map.of("text", "hi"));
        ItemDefinition definition = new ItemDefinition("sword", null, Material.STICK, null, null, null, null, null,
                null, false, null, null, null, null, null, null, null, null, null, null,
                Map.of(ItemTrigger.RIGHT_CLICK, List.of(action)), null, null, null, null, null, 0, 0, null);

        assertEquals(List.of(action), definition.actionsFor(ItemTrigger.RIGHT_CLICK));
        assertTrue(definition.actionsFor(ItemTrigger.LEFT_CLICK).isEmpty());
    }
}
