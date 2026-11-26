package com.sack.rpgroll.structures.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategorySettingsTest {

    private static YamlConfiguration yaml(String s) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(s);
            return y;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void defaultsMatchThePlan() {
        var s = CategorySettings.from(yaml(""), w -> fail(w));
        assertEquals(List.of("casas", "granjas_mobs", "granjas_auto", "parcelas", "decoracion", "infraestructura",
                "redstone", "especiales"), s.categories().stream().map(CategorySettings.Category::id).toList());
        assertEquals("minecraft:oak_door", s.byId("casas").orElseThrow().icon());
        assertEquals("minecraft:nether_star", s.byId("especiales").orElseThrow().icon());
        assertTrue(s.byId("nada").isEmpty());
    }

    @Test
    void readsFileAndSortsByOrder() {
        var s = CategorySettings.from(yaml("categories:\n  b:\n    icon: minecraft:rail\n    order: 2\n"
                + "  a:\n    icon: minecraft:wheat\n    order: 1\n  c:\n    order: 3\n"), w -> fail(w));
        assertEquals(List.of("a", "b", "c"), s.categories().stream().map(CategorySettings.Category::id).toList());
        assertEquals("minecraft:wheat", s.byId("a").orElseThrow().icon());
        assertEquals("minecraft:stone", s.byId("c").orElseThrow().icon());
    }

    @Test
    void duplicateCategoryIdKeepsFirstAndWarns() {
        List<String> warns = new ArrayList<>();
        var s = CategorySettings.from(yaml("categories:\n  Casas:\n    icon: minecraft:rail\n    order: 1\n"
                + "  casas:\n    icon: minecraft:wheat\n    order: 2\n"), warns::add);
        assertEquals(1, s.categories().size());
        assertEquals("minecraft:rail", s.byId("casas").orElseThrow().icon());
        assertEquals(1, warns.size());
        assertTrue(warns.get(0).contains("casas"));
    }
}
