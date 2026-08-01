package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassParser implements ContentParser<PlayerClass> {

    private final RPGRoll plugin;

    public ClassParser(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public PlayerClass parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        Map<StatType, Integer> baseAttributes = parseBaseAttributes(config, id);
        List<String> passiveTraits = config.getStringList("passive-traits");
        List<String> lore = config.getStringList("lore");
        String icon = parseIcon(config, id);

        return new PlayerClass(id, displayName, description, baseAttributes, passiveTraits, icon, lore);
    }

    private Map<StatType, Integer> parseBaseAttributes(YamlConfiguration config, String classId) {

        Map<StatType, Integer> attributes = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("base-attributes");

        if (section == null) {
            return attributes;
        }

        for (String key : section.getKeys(false)) {
            StatType stat = StatType.fromString(key);

            if (stat == null) {
                plugin.getLogger().warning("✘ Clase '" + classId + "': atributo desconocido '" + key + "', ignorado.");
                continue;
            }

            attributes.put(stat, section.getInt(key));
        }

        return attributes;
    }

    private String parseIcon(YamlConfiguration config, String classId) {

        String icon = config.getString("icon");

        if (icon == null || icon.isBlank()) {
            plugin.getLogger().warning(
                    "✘ Clase '" + classId + "' sin campo 'icon' (textura base64), usará cabeza sin textura.");
            return "";
        }

        return icon;
    }

}