package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/** Parser reutilizable para el bloque "requirements" de evoluciones/especializaciones/contenido secreto. */
public final class AscensionRequirementsParser {

    private AscensionRequirementsParser() {
    }

    public static AscensionRequirements parse(ConfigurationSection section) {

        if (section == null) {
            return AscensionRequirements.none();
        }

        int level = section.getInt("level", 0);
        int prestige = section.getInt("prestige", 0);
        String trait = section.getString("trait");
        var completedQuests = section.getStringList("completed-quests");

        Map<String, Integer> reputation = new HashMap<>();
        ConfigurationSection reputationSection = section.getConfigurationSection("reputation");

        if (reputationSection != null) {
            for (String faction : reputationSection.getKeys(false)) {
                reputation.put(faction, reputationSection.getInt(faction));
            }
        }

        return new AscensionRequirements(level, prestige, trait, completedQuests, reputation);
    }

}
