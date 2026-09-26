package com.sack.rpgroll.pass.season;

import com.sack.rpgroll.pass.requirement.LevelRequirements;
import com.sack.rpgroll.pass.reward.RewardParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

/** Lee {@code seasons/<id>.yml}. */
public final class SeasonLoader {

    private SeasonLoader() {
    }

    public static Optional<Season> load(File folder, String id, Consumer<String> warn) {

        File file = new File(folder, id + ".yml");

        if (!file.isFile()) {
            warn.accept("No existe seasons/" + id + ".yml; el pase queda cerrado.");
            return Optional.empty();
        }

        return Optional.ofNullable(parse(YamlConfiguration.loadConfiguration(file), id, warn));
    }

    public static Season parse(ConfigurationSection config, String fallbackId, Consumer<String> warn) {

        String id = config.getString("id", fallbackId);

        LocalDate start;
        LocalDate end;
        try {
            start = date(config.get("start"));
            end = date(config.get("end"));
        } catch (DateTimeParseException e) {
            warn.accept("Temporada " + id + ": start/end tienen que ser fechas AAAA-MM-DD.");
            return null;
        }

        int xpPerLevel = Math.max(1, config.getInt("xp-per-level", 1000));
        TreeMap<Integer, SeasonLevel> levels = new TreeMap<>();
        ConfigurationSection section = config.getConfigurationSection("levels");

        if (section != null) {
            for (String key : section.getKeys(false)) {

                int level;
                try {
                    level = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    warn.accept("Temporada " + id + ": nivel '" + key + "' no es un número.");
                    continue;
                }

                if (level < 1) {
                    warn.accept("Temporada " + id + ": los niveles empiezan en 1 (" + key + ").");
                    continue;
                }

                String where = "temporada " + id + " nivel " + level;
                levels.put(level, new SeasonLevel(level,
                        RewardParser.parseAll(list(section, key + ".free"), where + " (gratis)", warn),
                        RewardParser.parseAll(list(section, key + ".premium"), where + " (premium)", warn),
                        LevelRequirements.parse(section.getConfigurationSection(key + ".requirements"), where,
                                warn)));
            }
        }

        return new Season(id, config.getString("display-name", id), start, end, xpPerLevel,
                Collections.unmodifiableNavigableMap(levels));
    }

    /** YAML lee 2026-09-25 sin comillas como fecha (a medianoche UTC), no como texto. */
    private static LocalDate date(Object value) {

        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }

        return LocalDate.parse(value == null ? "" : value.toString());
    }

    private static List<String> list(ConfigurationSection section, String path) {
        return section.getStringList(path);
    }

}
