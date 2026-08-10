package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ModifierParser implements ContentParser<ModifierSet> {

    @Override
    public ModifierSet parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String rawType = config.getString("type", "RACE");
        ModifierSourceType type;

        try {
            type = ModifierSourceType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("modifier '" + id + "': type inválido: " + rawType);
        }

        Map<String, Double> values = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("values");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                values.put(key.toLowerCase(Locale.ROOT), section.getDouble(key));
            }
        }

        return new ModifierSet(id, type, values);
    }

}
