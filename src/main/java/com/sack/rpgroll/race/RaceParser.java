package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.content.ContentParser;
import com.sack.rpgroll.gameplay.stats.StatType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte un YamlConfiguration en una instancia de Race.
 * No conoce TraitRegistry — los passive-traits se cargan como IDs de texto.
 */
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
        Material icon = parseIcon(config, id);

        return new Race(id, displayName, description, baseAttributes, passiveTraits, icon, lore);
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
                plugin.getLogger().warning(
                        "✘ Raza '" + raceId + "': atributo desconocido '" + key + "', ignorado.");
                continue;
            }

            attributes.put(stat, section.getInt(key));
        }

        return attributes;
    }

    private Material parseIcon(YamlConfiguration config, String raceId) {

        String iconName = config.getString("icon");

        if (iconName == null || iconName.isBlank()) {
            plugin.getLogger().warning(
                    "✘ Raza '" + raceId + "' sin campo 'icon', usando BARRIER por defecto.");
            return Material.BARRIER;
        }

        Material material = Material.matchMaterial(iconName);

        if (material == null) {
            plugin.getLogger().warning(
                    "✘ Raza '" + raceId + "' tiene un icon inválido: '" + iconName + "', usando BARRIER por defecto.");
            return Material.BARRIER;
        }

        return material;
    }

}