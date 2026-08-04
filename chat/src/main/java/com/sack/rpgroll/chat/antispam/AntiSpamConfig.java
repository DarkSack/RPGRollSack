package com.sack.rpgroll.chat.antispam;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/** Configuración global de antispam (plugins/RPGRoll-Chat/config.yml, sección "antispam"). */
public record AntiSpamConfig(int floodMaxMessages, long floodWindowMillis, int repetitionThreshold,
        int maxCapsPercent, int minLengthForCapsCheck, List<String> bannedWords, List<String> adKeywords) {

    public static AntiSpamConfig defaults() {
        return new AntiSpamConfig(5, 10_000, 3, 70, 8,
                List.of(), List.of("discord.gg", "www.", "http://", "https://", ".com", ".net"));
    }

    public static AntiSpamConfig fromConfig(ConfigurationSection section) {

        if (section == null) {
            return defaults();
        }

        return new AntiSpamConfig(
                section.getInt("flood-max-messages", 5),
                section.getLong("flood-window-millis", 10_000),
                section.getInt("repetition-threshold", 3),
                section.getInt("max-caps-percent", 70),
                section.getInt("min-length-for-caps-check", 8),
                section.getStringList("banned-words"),
                section.getStringList("ad-keywords"));
    }

}
