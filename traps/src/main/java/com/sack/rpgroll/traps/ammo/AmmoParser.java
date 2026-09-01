package com.sack.rpgroll.traps.ammo;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.traps.core.TrapAction;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AmmoParser implements ContentParser<AmmoDefinition> {

    @Override
    public AmmoDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Material icon = parseMaterial(config.getString("icon"), Material.ARROW);

        Integer customModelData = config.contains("custom-model-data")
                ? config.getInt("custom-model-data")
                : null;

        return new AmmoDefinition(
                id,
                config.getString("display-name"),
                config.getString("description"),
                icon,
                customModelData,
                parseImpact(config),
                config.getInt("stack-size", 1));
    }

    /** Sin sección "impact" la munición hereda la acción de la torreta. */
    private TrapAction parseImpact(YamlConfiguration config) {

        String type = config.getString("impact.type");

        if (type == null || type.isBlank()) {
            return null;
        }

        Map<String, String> params = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("impact.params");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                params.put(key, String.valueOf(section.get(key)));
            }
        }

        return new TrapAction(type.trim().toUpperCase(Locale.ROOT), params);
    }

    private Material parseMaterial(String raw, Material fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
