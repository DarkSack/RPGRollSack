package com.sack.rpgroll.worldforge.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The worlds that {@code bukkit.yml} generates with WorldForge. If WorldForge cannot give Paper
 * their generator (no license, broken preset), Paper logs it and generates them with its normal
 * terrain, and fixing those chunks means regenerating them.
 */
final class BukkitWorlds {

    private BukkitWorlds() {
    }

    /** World name → preset id ("" when bukkit.yml names none), in file order. */
    static Map<String, String> using(ConfigurationSection bukkit, String pluginName) {
        Map<String, String> out = new LinkedHashMap<>();
        ConfigurationSection worlds = bukkit.getConfigurationSection("worlds");
        if (worlds == null) {
            return out;
        }
        for (String world : worlds.getKeys(false)) {
            ConfigurationSection section = worlds.getConfigurationSection(world);
            String generator = section == null ? null : section.getString("generator");
            if (generator == null || generator.isBlank()) {
                continue;
            }
            String[] split = generator.split(":", 2);
            // Paper looks plugins up this way: spaces as _ and without caring about case.
            if (lookup(split[0]).equals(lookup(pluginName))) {
                out.put(world, split.length > 1 ? split[1].trim() : "");
            }
        }
        return out;
    }

    private static String lookup(String name) {
        return name.trim().replace(' ', '_').toLowerCase(Locale.ENGLISH);
    }
}
