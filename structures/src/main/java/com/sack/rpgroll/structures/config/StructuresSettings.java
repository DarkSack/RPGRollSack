package com.sack.rpgroll.structures.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.function.Consumer;

/** Ajustes de {@code config.yml}. */
public record StructuresSettings(String language, String menusPack, BreakPolicy breakPolicy) {

    /** Qué pasa al romper una mesa con una obra activa. */
    public enum BreakPolicy { BLOCK, PAUSE, CANCEL }

    public static StructuresSettings from(ConfigurationSection root, Consumer<String> warn) {
        String language = root.getString("language", "es");
        String pack = root.getString("menus.pack", "");
        String raw = root.getString("break-policy", "block");
        BreakPolicy policy = BreakPolicy.BLOCK;
        if (!raw.isBlank()) {
            try {
                policy = BreakPolicy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                warn.accept("break-policy: '" + raw + "' no es block, pause ni cancel; uso block.");
            }
        }
        return new StructuresSettings(language, pack, policy);
    }
}
