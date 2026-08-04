package com.sack.rpgroll.items.core;

public enum RecipeType {
    SHAPED,
    SHAPELESS,
    FURNACE,
    SMITHING,
    STONECUTTER,
    // Tipos que requieren integración de otro addon para "cumplirse" — el
    // motor los registra como datos y expone un punto de extensión (ver
    // com.sack.rpgroll.items.recipe.CustomRecipeRegistry) en vez de una
    // implementación propia, ya que su lógica de negocio pertenece a esos
    // addons (NPC, trabajos, quests), no a RPGRoll-Items.
    NPC,
    PROFESSION,
    QUEST
}
