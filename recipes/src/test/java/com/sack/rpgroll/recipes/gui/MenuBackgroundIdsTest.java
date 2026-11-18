package com.sack.rpgroll.recipes.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("recetario_categorias", CategoriesGUI.BACKGROUND);
        assertEquals("recetario_plugins", PluginsGUI.BACKGROUND);
        assertEquals("recetario_catalogo", CatalogGUI.BACKGROUND);
        assertEquals("recetario_receta", RecipeGUI.BACKGROUND);
        assertEquals("recipes", RecipesMenus.MODULE);
    }
}
