package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.ConfigurationSection;

/** Contraparte de {@link AscensionRequirementsParser} — escribe el bloque "requirements" a YAML. */
public final class AscensionRequirementsWriter {

    private AscensionRequirementsWriter() {
    }

    public static void write(ConfigurationSection parent, String key, AscensionRequirements requirements) {

        if (requirements == null) {
            return;
        }

        ConfigurationSection section = parent.createSection(key);
        section.set("level", requirements.level());
        section.set("prestige", requirements.prestige());

        if (requirements.trait() != null) {
            section.set("trait", requirements.trait());
        }

        section.set("completed-quests", requirements.completedQuests());

        ConfigurationSection reputationSection = section.createSection("reputation");
        requirements.reputation().forEach(reputationSection::set);
    }

}
