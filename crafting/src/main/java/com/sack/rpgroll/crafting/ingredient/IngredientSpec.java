package com.sack.rpgroll.crafting.ingredient;

/**
 * Un ingrediente requerido por una receta: qué buscar ({@code type}+{@code value}),
 * cuántos consumir, y opcionalmente una calidad mínima (id de {@code QualityDefinition})
 * que el ítem entregado debe cumplir o superar.
 */
public record IngredientSpec(
        IngredientType type,
        String value,
        int amount,
        String minQuality) {

    public IngredientSpec {
        amount = Math.max(1, amount);
    }

    public static IngredientSpec material(String materialName, int amount) {
        return new IngredientSpec(IngredientType.MATERIAL, materialName, amount, null);
    }

    public static IngredientSpec tag(String tagName, int amount) {
        return new IngredientSpec(IngredientType.TAG, tagName, amount, null);
    }

    public static IngredientSpec itemId(String id, int amount) {
        return new IngredientSpec(IngredientType.ITEM_ID, id, amount, null);
    }

    public boolean hasMinQuality() {
        return minQuality != null && !minQuality.isBlank();
    }

}
