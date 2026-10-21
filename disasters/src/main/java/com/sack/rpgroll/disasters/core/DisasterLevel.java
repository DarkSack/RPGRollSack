package com.sack.rpgroll.disasters.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.TreeMap;

/**
 * Un nivel de desastre (1 = flojo ... 6 = catastrófico). Al salir solo se sortea por {@code weight};
 * el nivel multiplica la fuerza de los fenómenos ({@code intensity}), su tamaño ({@code size}: radios,
 * alturas, largos) y la duración ({@code duration}).
 */
public record DisasterLevel(double weight, double intensity, double size, double duration) {

    public DisasterLevel {
        weight = Math.max(0, weight);
        intensity = intensity > 0 ? intensity : 1;
        size = size > 0 ? size : 1;
        duration = duration > 0 ? duration : 1;
    }

    /** Lee {@code 1: {weight: 30, intensity: 0.6, size: 0.8, duration: 0.7}, 2: ...}; sin sección, vacío. */
    public static Map<Integer, DisasterLevel> parse(ConfigurationSection section) {

        Map<Integer, DisasterLevel> levels = new TreeMap<>();
        if (section == null) {
            return levels;
        }

        for (String key : section.getKeys(false)) {
            int number;
            try {
                number = Integer.parseInt(key.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("nivel '" + key + "' no es un número");
            }
            ConfigurationSection level = section.getConfigurationSection(key);
            if (level == null || number < 1) {
                throw new IllegalArgumentException("nivel '" + key + "' mal escrito");
            }
            levels.put(number, new DisasterLevel(level.getDouble("weight", 1), level.getDouble("intensity", 1),
                    level.getDouble("size", 1), level.getDouble("duration", 1)));
        }
        return levels;
    }

    /** El nivel que toca con {@code roll} en [0, 1), repartido por peso; null si no hay ninguno con peso. */
    public static Integer pick(Map<Integer, DisasterLevel> levels, double roll) {

        double total = levels.values().stream().mapToDouble(DisasterLevel::weight).sum();
        if (total <= 0) {
            return null;
        }

        double target = roll * total;
        Integer last = null;
        for (var entry : new TreeMap<>(levels).entrySet()) {
            if (entry.getValue().weight() <= 0) {
                continue;
            }
            last = entry.getKey();
            target -= entry.getValue().weight();
            if (target < 0) {
                return last;
            }
        }
        return last;
    }

}
