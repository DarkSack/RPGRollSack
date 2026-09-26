package com.sack.rpgroll.items.core;

import java.util.List;
import java.util.Map;

/**
 * Receta para obtener el ítem. {@code shape}+{@code key} se usan para
 * SHAPED; {@code ingredients} para SHAPELESS/FURNACE/STONECUTTER;
 * {@code baseMaterial} para SMITHING (la plantilla/base a transformar);
 * FURNACE y STONECUTTER también lo aceptan como entrada si no hay
 * {@code ingredients}. {@code amount} es cuántos ítems da (1 a 64).
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
        String sourceId,
        int amount) {

    public ItemRecipeDef {
        shape = shape == null ? List.of() : List.copyOf(shape);
        key = key == null ? Map.of() : Map.copyOf(key);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        amount = Math.max(1, Math.min(64, amount));
    }

    public ItemRecipeDef(RecipeType type, List<String> shape, Map<String, String> key, List<String> ingredients,
            String baseMaterial, int cookingTimeTicks, String sourceId) {
        this(type, shape, key, ingredients, baseMaterial, cookingTimeTicks, sourceId, 1);
    }

    /** La entrada de FURNACE/STONECUTTER: el primer ingrediente o, si no hay, {@code base-material}. */
    public String singleInput() {
        return !ingredients.isEmpty() ? ingredients.get(0) : baseMaterial;
    }

}
