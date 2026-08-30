package com.sack.rpgroll.npcs.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcMenuParserTest {

    private final NpcMenuParser parser = new NpcMenuParser();

    private YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void missingIdThrows() {
        YamlConfiguration config = load("title: Shop");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalMenuFallsBackToDefaults() {
        YamlConfiguration config = load("id: shop");
        NpcMenuDefinition def = parser.parse(config);

        assertEquals("shop", def.id());
        assertEquals("shop", def.title());
        assertEquals(3, def.rows());
        assertTrue(def.items().isEmpty());
    }

    @Test
    void itemsWithoutSlotOrMaterialAreSkipped() {
        YamlConfiguration config = load("""
                id: shop
                items:
                  - material: DIAMOND_SWORD
                  - slot: 0
                  - slot: 1
                    material: GOLD_INGOT
                """);

        NpcMenuDefinition def = parser.parse(config);

        assertEquals(1, def.items().size());
        assertEquals(1, def.items().get(0).slot());
        assertEquals("GOLD_INGOT", def.items().get(0).material());
    }

    @Test
    void parsesItemLoreAndActions() {
        YamlConfiguration config = load("""
                id: shop
                items:
                  - slot: 0
                    material: DIAMOND
                    name: Diamond
                    lore:
                      - "Line1"
                      - "Line2"
                    actions:
                      - type: MESSAGE
                        value: bought!
                """);

        NpcMenuItem item = parser.parse(config).items().get(0);

        assertEquals("Diamond", item.displayName());
        assertEquals(2, item.lore().size());
        assertEquals(1, item.actions().size());
        assertEquals(NpcAction.NpcActionType.MESSAGE, item.actions().get(0).type());
    }

    @Test
    void invalidActionTypeInItemIsSkipped() {
        YamlConfiguration config = load("""
                id: shop
                items:
                  - slot: 0
                    material: DIAMOND
                    actions:
                      - type: NOT_A_TYPE
                        value: x
                """);

        assertTrue(parser.parse(config).items().get(0).actions().isEmpty());
    }
}
