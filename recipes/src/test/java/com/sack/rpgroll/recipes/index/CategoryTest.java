package com.sack.rpgroll.recipes.index;

import com.sack.rpgroll.recipes.index.RecipeIndex.CatalogItem;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTest {

    private static CatalogItem producedBy(String... sources) {
        return new CatalogItem(null, "rpgroll-items:x", Set.of(), Set.of(sources), "", "", Set.of(), false);
    }

    @Test
    void propiosSonLosPluginsDeRpgrollYSackito() {
        assertTrue(Category.ownSource("RPGRoll-Items"));
        assertTrue(Category.ownSource("rpgroll-machines"));
        assertTrue(Category.ownSource("SackitoNetwork"));
        assertFalse(Category.ownSource("Minecraft"));
        assertFalse(Category.ownSource("tooltrims"));
    }

    @Test
    void fabricableEnSackitoSoloSiUnaRecetaPropiaLoProduce() {
        assertTrue(Category.OWN.matches(producedBy("Minecraft", "RPGRoll-Crafting")));
        assertFalse(Category.OWN.matches(producedBy("Minecraft", "master_cutter")));
        // Materia prima de un plugin que nada produce: sale en "Del servidor", no aquí.
        assertFalse(Category.OWN.matches(producedBy()));
    }
}
