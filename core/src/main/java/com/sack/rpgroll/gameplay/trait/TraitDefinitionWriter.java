package com.sack.rpgroll.gameplay.trait;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa un {@link Trait} completo de vuelta a YAML — inverso de {@link TraitParser}. */
public class TraitDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public TraitDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Trait trait) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", trait.id());
        config.set("name", trait.name());
        config.set("description", trait.description());
        config.set("required-level", trait.requiredLevel());

        TraitEffect effect = trait.effect();
        config.set("effect.strength-bonus", effect.strengthBonus());
        config.set("effect.dexterity-bonus", effect.dexterityBonus());
        config.set("effect.constitution-bonus", effect.constitutionBonus());
        config.set("effect.intelligence-bonus", effect.intelligenceBonus());
        config.set("effect.wisdom-bonus", effect.wisdomBonus());
        config.set("effect.charisma-bonus", effect.charismaBonus());
        config.set("effect.health-bonus", effect.healthBonus());
        config.set("effect.mana-bonus", effect.manaBonus());
        config.set("effect.damage-bonus", effect.damageBonus());
        config.set("effect.defense-bonus", effect.defenseBonus());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, trait.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando trait '" + trait.id() + "': " + e.getMessage());
        }
    }

}
