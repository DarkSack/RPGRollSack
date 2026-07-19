package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.content.ContentParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Convierte un YamlConfiguration (traits/vision_nocturna.yml, etc.) en una
 * instancia de Trait.
 */
public class TraitParser implements ContentParser<Trait> {

    @Override
    public Trait parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String name = config.getString("name", id);
        String description = config.getString("description", "");
        int requiredLevel = config.getInt("required-level", 1);

        TraitEffect effect = parseEffect(config.getConfigurationSection("effect"));

        return new Trait(id, name, description, requiredLevel, effect);
    }

    private TraitEffect parseEffect(ConfigurationSection section) {

        if (section == null) {
            return TraitEffect.empty();
        }

        return new TraitEffect(
                section.getInt("strength-bonus", 0),
                section.getInt("dexterity-bonus", 0),
                section.getInt("constitution-bonus", 0),
                section.getInt("intelligence-bonus", 0),
                section.getInt("wisdom-bonus", 0),
                section.getInt("charisma-bonus", 0),
                section.getInt("health-bonus", 0),
                section.getInt("mana-bonus", 0),
                section.getDouble("damage-bonus", 0.0),
                section.getDouble("defense-bonus", 0.0));
    }

}