package com.sack.rpgroll.seasons.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SeasonDefinitionWriter {

    private final File folder;

    public SeasonDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "seasons");
    }

    public void save(Season season) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", season.id());
        config.set("display-name", season.displayName());
        config.set("icon", season.icon());
        config.set("color", season.color());
        config.set("description", season.description());
        config.set("duration-amount", season.durationAmount());
        config.set("duration-unit", season.durationUnit().name());
        config.set("exclusive-boss", season.exclusiveBossId());
        config.set("world-events", List.copyOf(season.worldEventIds()));
        config.set("world-event-daily-chance", season.worldEventDailyChance());
        config.set("tags", List.copyOf(season.tags()));
        config.set("vegetation-effects", season.vegetationEffects().stream().map(Enum::name).toList());

        ClimateProfile climate = season.climate();
        config.set("climate.rain-chance", climate.rainChance());
        config.set("climate.storm-chance", climate.stormChance());
        config.set("climate.snow-chance", climate.snowChance());
        config.set("climate.fog-chance", climate.fogChance());
        config.set("climate.base-temperature", climate.baseTemperature());
        config.set("climate.temperature-variance", climate.temperatureVariance());
        config.set("climate.wind-strength", climate.windStrength());
        config.set("climate.humidity", climate.humidity());
        config.set("climate.heatwave-chance", climate.heatwaveChance());
        config.set("climate.thunderstorm-chance", climate.thunderstormChance());

        for (var entry : season.biomeTemperatureModifiers().entrySet()) {
            config.set("biome-temperature-modifiers." + entry.getKey(), entry.getValue());
        }

        List<Map<String, Object>> subSeasons = new ArrayList<>();
        for (SubSeason subSeason : season.subSeasons()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", subSeason.id());
            map.put("display-name", subSeason.displayName());
            map.put("duration-amount", subSeason.durationAmount());
            map.put("duration-unit", subSeason.durationUnit().name());
            if (subSeason.temperatureOverride() != null) {
                map.put("temperature-override", subSeason.temperatureOverride());
            }
            subSeasons.add(map);
        }
        config.set("sub-seasons", subSeasons);

        List<Map<String, Object>> mobModifiers = new ArrayList<>();
        for (SeasonMobModifier modifier : season.mobModifiers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("mob-id", modifier.mobId());
            map.put("extra-spawn-chance", modifier.extraSpawnChance());
            mobModifiers.add(map);
        }
        config.set("mob-modifiers", mobModifiers);

        try {
            folder.mkdirs();
            config.save(new File(folder, season.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la estación " + season.id(), e);
        }
    }

}
