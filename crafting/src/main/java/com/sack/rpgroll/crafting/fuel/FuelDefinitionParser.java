package com.sack.rpgroll.crafting.fuel;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class FuelDefinitionParser implements ContentParser<FuelDefinition> {

    @Override
    public FuelDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String materialOrItemId = config.getString("material-or-item-id");
        if (materialOrItemId == null || materialOrItemId.isBlank()) {
            throw new IllegalArgumentException("combustible '" + id + "' sin campo obligatorio 'material-or-item-id'");
        }

        return new FuelDefinition(
                id,
                config.getString("display-name", id),
                config.getString("icon", "COAL"),
                materialOrItemId,
                config.getBoolean("is-custom-item", false),
                config.getInt("burn-ticks", 200),
                config.getInt("consume-amount", 1));
    }

}
