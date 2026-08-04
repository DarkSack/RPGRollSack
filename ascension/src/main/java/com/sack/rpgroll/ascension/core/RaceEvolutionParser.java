package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RaceEvolutionParser implements ContentParser<RaceEvolution> {

    @Override
    public RaceEvolution parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String baseRace = config.getString("base-race");
        if (baseRace == null || baseRace.isBlank()) {
            throw new IllegalArgumentException("evolución '" + id + "' sin campo obligatorio 'base-race'");
        }

        String displayName = config.getString("display-name", id);
        AscensionRequirements requirements = AscensionRequirementsParser.parse(
                config.getConfigurationSection("requirements"));

        Map<String, Double> statBonus = parseNumberMap(config.getConfigurationSection("stats"));
        Map<String, Double> affinityBonus = parseNumberMap(config.getConfigurationSection("affinities"));
        Map<String, Double> resistances = parseNumberMap(config.getConfigurationSection("resistances"));
        Map<String, Double> weaknesses = parseNumberMap(config.getConfigurationSection("weaknesses"));

        return new RaceEvolution(id, baseRace.toLowerCase(Locale.ROOT), displayName, requirements, statBonus,
                config.getStringList("traits"), config.getStringList("skills"), affinityBonus, resistances,
                weaknesses, config.getStringList("professions"));
    }

    private Map<String, Double> parseNumberMap(ConfigurationSection section) {

        Map<String, Double> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            if (section.get(key) instanceof Number number) {
                result.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
            }
        }

        return result;
    }

}
