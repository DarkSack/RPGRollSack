package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FishSpeciesParser implements ContentParser<FishSpecies> {

    @Override
    public FishSpecies parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new FishSpecies(
                id,
                config.getString("display-name", id),
                config.getString("icon", "COD"),
                config.getInt("custom-model-data", 0),
                config.getString("description", ""),
                parseEnum(FishCategory.class, config.getString("category"), FishCategory.FRESHWATER),
                parseEnum(FishRarity.class, config.getString("rarity"), FishRarity.COMMON),
                parseEnumSet(WaterType.class, config.getStringList("water-types")),
                lowercaseSet(config.getStringList("biomes")),
                parseEnumSet(DepthRequirement.class, config.getStringList("depths")),
                config.getDouble("min-weight", 0.5),
                config.getDouble("max-weight", 2.0),
                config.getDouble("min-length", 10),
                config.getDouble("max-length", 40),
                config.getDouble("base-price", 5),
                config.getInt("base-experience", 5),
                parseEnum(FishBehaviorType.class, config.getString("behavior"), FishBehaviorType.SLOW),
                lowercaseSet(config.getStringList("allowed-seasons")),
                parseEnumSet(WeatherType.class, config.getStringList("allowed-weathers")),
                parseEnumSet(TimeRequirement.class, config.getStringList("allowed-times")),
                lowercaseSet(config.getStringList("attracted-by-bait-tags")),
                config.getBoolean("legendary", false),
                config.getInt("required-level", 0),
                config.getBoolean("requires-full-moon", false),
                config.getString("required-bait"),
                config.getString("catch-effect"),
                config.getString("catch-status-effect"));
    }

    private Set<String> lowercaseSet(List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

    private <E extends Enum<E>> Set<E> parseEnumSet(Class<E> type, List<String> raw) {

        Set<E> result = new HashSet<>();

        for (String entry : raw) {
            try {
                result.add(Enum.valueOf(type, entry.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
