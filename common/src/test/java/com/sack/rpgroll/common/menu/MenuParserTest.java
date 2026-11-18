package com.sack.rpgroll.common.menu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuParserTest {

    private final MenuParser parser = new MenuParser();

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
        MenuDefinition def = parser.parse(config);

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

        MenuDefinition def = parser.parse(config);

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

        MenuItem item = parser.parse(config).items().get(0);

        assertEquals("Diamond", item.displayName());
        assertEquals(2, item.lore().size());
        assertEquals(1, item.actions().size());
        assertEquals(MenuAction.ActionType.MESSAGE, item.actions().get(0).type());
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

    @Test
    void readsBackgroundAndModel() {
        MenuDefinition def = parser.parse(load("""
                id: servidor
                background: sackito:menus/brujula
                items:
                  - slot: 4
                    material: COMPASS
                    model: invisible
                """));

        assertEquals("sackito:menus/brujula", def.background());
        assertEquals("invisible", def.items().get(0).model());
    }

    @Test
    void menuWithoutBackgroundStaysPlain() {
        MenuDefinition def = parser.parse(load("""
                id: servidor
                filler: GRAY_STAINED_GLASS_PANE
                items:
                  - slot: 4
                    material: COMPASS
                """));

        assertNull(def.background());
        assertNull(def.items().get(0).model());
        assertEquals("GRAY_STAINED_GLASS_PANE", def.filler());
    }

    @Test
    void prefixOfBackground() {
        assertEquals("sackito:menus/invisible", MenuGUI.modelKey("sackito:menus/brujula", "invisible"));
        assertNull(MenuGUI.modelKey(null, "invisible"));
        assertNull(MenuGUI.modelKey("sackito:menus/brujula", null));
        assertNull(MenuGUI.modelKey("sin barra", "invisible"));
    }
}
