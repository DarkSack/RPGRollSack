package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RacePhysicalModifiers;
import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.api.stats.StatType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceParser implements ContentParser<Race> {

    private final RPGRoll plugin;

    public RaceParser(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public Race parse(YamlConfiguration config) {

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
        RacePhysicalModifiers physicalModifiers = parsePhysicalModifiers(config);

        return new Race(id, displayName, description, baseAttributes, passiveTraits, icon, lore, physicalModifiers);
    }

    private Map<StatType, Integer> parseBaseAttributes(YamlConfiguration config, String raceId) {

        Map<StatType, Integer> attributes = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("base-attributes");

        if (section == null) {
            return attributes;
        }

        for (String key : section.getKeys(false)) {
            StatType stat = StatType.fromString(key);

            if (stat == null) {
                plugin.getLogger().warning("✘ Raza '" + raceId + "': atributo desconocido '" + key + "', ignorado.");
                continue;
            }

            attributes.put(stat, section.getInt(key));
        }

        return attributes;
    }

    private String parseIcon(YamlConfiguration config, String raceId) {

        String icon = config.getString("icon");

        if (icon == null || icon.isBlank()) {
            plugin.getLogger().warning(
                    "✘ Raza '" + raceId + "' sin campo 'icon' (textura base64), usará cabeza sin textura.");
            return "";
        }

        return icon;
    }

    private RacePhysicalModifiers parsePhysicalModifiers(YamlConfiguration config) {

        ConfigurationSection section = config.getConfigurationSection("physical");

        if (section == null) {
            return RacePhysicalModifiers.none();
        }

        return new RacePhysicalModifiers(
                section.getDouble("scale", 1.0),
                section.getDouble("movement-speed-percent", 0.0),
                section.getDouble("extra-health", 0.0),
                section.getDouble("knockback-resistance", 0.0));
    }

}