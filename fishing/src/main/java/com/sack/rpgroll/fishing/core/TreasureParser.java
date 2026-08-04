package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class TreasureParser implements ContentParser<Treasure> {

    @Override
    public Treasure parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        FishRarity rarity;

        try {
            rarity = FishRarity.valueOf(config.getString("rarity", "UNCOMMON").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            rarity = FishRarity.UNCOMMON;
        }

        return new Treasure(
                id,
                config.getString("display-name", id),
                config.getString("icon", "CHEST"),
                config.getString("description", ""),
                rarity,
                config.getString("reward-material", "CHEST"),
                config.getInt("reward-amount", 1),
                config.getDouble("weight", 1.0));
    }

}
