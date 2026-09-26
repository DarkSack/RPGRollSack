package com.sack.rpgroll.items.core;

/**
 * Una apariencia alternativa del ítem — cambia material/nombre/custom-model-data
 * de visualización, nunca las estadísticas. El jugador puede ciclar entre
 * las skins definidas sin perder el resto del estado del ítem.
 */
public record ItemSkin(String id, String displayName, String material, Integer customModelData, String itemModel) {

    public ItemSkin(String id, String displayName, String material, Integer customModelData) {
        this(id, displayName, material, customModelData, null);
    }
}
