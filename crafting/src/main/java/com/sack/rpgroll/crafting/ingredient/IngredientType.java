package com.sack.rpgroll.crafting.ingredient;

public enum IngredientType {
    /** Material vanilla exacto (p.ej. IRON_INGOT). */
    MATERIAL,
    /** Tag vanilla de ítems (p.ej. minecraft:planks) — cualquier material del tag sirve. */
    TAG,
    /** Ítem personalizado por id — propio de Crafting o de RPGRoll-Items (ver ItemIdentity). */
    ITEM_ID,
    /** Cualquier ítem sirve, solo importa la cantidad (comodín). */
    ANY
}
