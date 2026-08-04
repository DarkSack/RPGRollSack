package com.sack.rpgroll.items.recipe;

import com.sack.rpgroll.items.core.RecipeType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Punto de extensión para los tipos de receta que dependen de otro addon
 * para "cumplirse" (NPC, PROFESSION, QUEST) — RPGRoll-Items solo guarda
 * los datos ({@link com.sack.rpgroll.items.core.ItemRecipeDef}), el addon
 * correspondiente decide cuándo el jugador la cumple y llama a
 * {@code ItemsPlugin#getItemManager()} para entregar el ítem resultante.
 */
public class CustomRecipeRegistry {

    private final Map<RecipeType, Boolean> supported = new EnumMap<>(RecipeType.class);

    public void markSupported(RecipeType type) {
        supported.put(type, true);
    }

    public boolean isSupported(RecipeType type) {
        return supported.getOrDefault(type, false);
    }

}
