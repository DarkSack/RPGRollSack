package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SeasonParser implements ContentParser<Season> {

    @Override
    public Season parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        DurationUnit durationUnit = parseEnum(DurationUnit.class, config.getString("duration-unit"),
                DurationUnit.MINECRAFT_DAYS);

        return new Season(
                id,
                config.getString("display-name", id),
                config.getString("icon", "SUNFLOWER"),
                config.getString("color", "WHITE"),
                config.getString("description", ""),
                config.getInt("duration-amount", 7),
                durationUnit,
                parseClimate(config),
                parseSubSeasons(config.getMapList("sub-seasons")),
                parseBiomeModifiers(config),
                parseVegetationEffects(config.getStringList("vegetation-effects")),
                parseMobModifiers(config.getMapList("mob-modifiers")),
                config.getString("exclusive-boss"),
                config.getStringList("world-events"),
                config.getDouble("world-event-daily-chance", 0.0),
                Set.copyOf(config.getStringList("tags")));
    }

    private ClimateProfile parseClimate(YamlConfiguration config) {

        var section = config.getConfigurationSection("climate");

        if (section == null) {
            return ClimateProfile.temperate();
        }

        return new ClimateProfile(
                section.getDouble("rain-chance", 0.3),
                section.getDouble("storm-chance", 0.05),
                section.getDouble("snow-chance", 0.0),
                section.getDouble("fog-chance", 0.1),
                section.getDouble("base-temperature", 15.0),
                section.getDouble("temperature-variance", 3.0),
                section.getDouble("wind-strength", 0.3),
                section.getDouble("humidity", 0.5),
                section.getDouble("heatwave-chance", 0.02),
                section.getDouble("thunderstorm-chance", 0.02));
    }

    private List<SubSeason> parseSubSeasons(List<?> raw) {

        List<SubSeason> subSeasons = new ArrayList<>();

        for (Object rawEntry : raw) {

            if (!(rawEntry instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawId = map.get("id");
            if (rawId == null) {
                continue;
            }

            DurationUnit unit = parseEnum(DurationUnit.class, stringOrNull(map.get("duration-unit")),
                    DurationUnit.MINECRAFT_DAYS);
            int amount = map.get("duration-amount") instanceof Number number ? number.intValue() : 1;

            Double temperatureOverride = null;
            if (map.get("temperature-override") instanceof Number number) {
                temperatureOverride = number.doubleValue();
            }

            subSeasons.add(new SubSeason(rawId.toString(), stringOrNull(map.get("display-name")), amount, unit,
                    temperatureOverride));
        }

        return subSeasons;
    }

    private Map<String, Double> parseBiomeModifiers(YamlConfiguration config) {

        var section = config.getConfigurationSection("biome-temperature-modifiers");

        if (section == null) {
            return Map.of();
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (String key : section.getKeys(false)) {
            result.put(key.toLowerCase(Locale.ROOT), section.getDouble(key));
        }

        return result;
    }

    private Set<VegetationEffectType> parseVegetationEffects(List<String> raw) {

        Set<VegetationEffectType> result = new java.util.LinkedHashSet<>();

        for (String entry : raw) {
            try {
                result.add(VegetationEffectType.valueOf(entry.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return result;
    }

    private List<SeasonMobModifier> parseMobModifiers(List<?> raw) {

        List<SeasonMobModifier> modifiers = new ArrayList<>();

        for (Object rawEntry : raw) {

            if (!(rawEntry instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawMobId = map.get("mob-id");
            if (rawMobId == null) {
                continue;
            }

            double chance = map.get("extra-spawn-chance") instanceof Number number ? number.doubleValue() : 0.1;
            modifiers.add(new SeasonMobModifier(rawMobId.toString(), chance));
        }

        return modifiers;
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

    private String stringOrNull(Object raw) {
        return raw == null ? null : raw.toString();
    }

}
