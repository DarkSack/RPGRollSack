package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GemParser implements ContentParser<Gem> {

    @Override
    public Gem parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String type = config.getString("type", "GENERIC");

        Map<String, Double> statBonus = new HashMap<>();
        ConfigurationSection statsSection = config.getConfigurationSection("stats");

        if (statsSection != null) {
            for (String key : statsSection.getKeys(false)) {
                if (statsSection.get(key) instanceof Number number) {
                    statBonus.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
                }
            }
        }

        return new Gem(id, displayName, type, statBonus);
    }

}
