package com.sack.rpgroll.items.core;

import java.util.List;
import java.util.Map;

/**
 * Receta para obtener el ítem. {@code shape}+{@code key} se usan para
 * SHAPED; {@code ingredients} para SHAPELESS/FURNACE/STONECUTTER;
 * {@code baseMaterial} para SMITHING (la plantilla/base a transformar).
 * Para NPC/PROFESSION/QUEST solo {@code sourceId} importa — el addon
 * correspondiente decide cómo se cumple.
 */
public record ItemRecipeDef(
        RecipeType type,
        List<String> shape,
        Map<String, String> key,
        List<String> ingredients,
        String baseMaterial,
        int cookingTimeTicks,
        String sourceId) {

    public ItemRecipeDef {
        shape = shape == null ? List.of() : List.copyOf(shape);
        key = key == null ? Map.of() : Map.copyOf(key);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }

}
