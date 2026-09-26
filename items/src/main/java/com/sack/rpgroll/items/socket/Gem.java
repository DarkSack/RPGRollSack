package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.Map;

/**
 * Una gema/runa/cristal insertable en un {@link com.sack.rpgroll.items.core.SocketDefinition}
 * compatible. Modifica los stats del ítem que la contiene mientras esté
 * insertada.
 *
 * <p>{@code material} es el ítem base (esmeralda si no se indica) e
 * {@code itemModel} el modelo del resource pack ({@code item_model}), para
 * que cada gema tenga su ícono en Java y, vía Geyser, en Bedrock.
 */
public record Gem(String id, String displayName, String type, Map<String, Double> statBonus, Material material,
        String itemModel) implements RPGContent {

    public Gem {
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
        material = material == null ? Material.EMERALD : material;
        itemModel = itemModel == null || itemModel.isBlank() ? null : itemModel;
    }

    public Gem(String id, String displayName, String type, Map<String, Double> statBonus) {
        this(id, displayName, type, statBonus, null, null);
    }

}
