package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RuneParser implements ContentParser<Rune> {

    @Override
    public Rune parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String rawType = config.getString("type");
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("runa '" + id + "' sin campo obligatorio 'type'");
        }

        RuneModifierType type;

        try {
            type = RuneModifierType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("runa '" + id + "' tiene un type inválido: " + rawType);
        }

        Map<String, String> params = new LinkedHashMap<>();
        var section = config.getConfigurationSection("params");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                params.put(key, String.valueOf(section.get(key)));
            }
        }

        return new Rune(
                id,
                config.getString("display-name", id),
                config.getString("icon", "EMERALD"),
                config.getString("description", ""),
                type,
                params);
    }

}
