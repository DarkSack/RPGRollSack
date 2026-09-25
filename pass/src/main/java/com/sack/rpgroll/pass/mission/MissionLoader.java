package com.sack.rpgroll.pass.mission;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Lee la sección {@code missions} de missions.yml. */
public final class MissionLoader {

    private MissionLoader() {
    }

    public static List<Mission> parse(ConfigurationSection missions, Consumer<String> warn) {

        List<Mission> result = new ArrayList<>();

        if (missions == null) {
            return result;
        }

        for (String id : missions.getKeys(false)) {

            ConfigurationSection section = missions.getConfigurationSection(id);

            if (section == null) {
                continue;
            }

            try {
                MissionScope scope = MissionScope.valueOf(section.getString("scope", "DAILY").toUpperCase(Locale.ROOT));
                MissionType type = MissionType.valueOf(section.getString("type", "").toUpperCase(Locale.ROOT));
                int amount = Math.max(1, section.getInt("amount", 1));
                int xp = Math.max(0, section.getInt("xp", 100));
                String target = section.getString("target", "").trim();

                result.add(new Mission(id, section.getString("name", id), scope, type, target, amount, xp));
            } catch (IllegalArgumentException e) {
                warn.accept("Misión " + id + " ignorada: scope o type desconocido.");
            }
        }

        return result;
    }

}
