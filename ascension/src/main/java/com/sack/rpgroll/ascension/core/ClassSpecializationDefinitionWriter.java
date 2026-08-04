package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ClassSpecializationDefinitionWriter {

    private final File folder;

    public ClassSpecializationDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "specializations");
    }

    public void save(ClassSpecialization specialization) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", specialization.id());
        config.set("base-class", specialization.baseClass());
        config.set("display-name", specialization.displayName());
        AscensionRequirementsWriter.write(config, "requirements", specialization.requirements());

        ConfigurationSection stats = config.createSection("stats");
        specialization.statBonus().forEach(stats::set);

        config.set("restrictions", specialization.restrictions());
        config.set("exclusive-equipment", specialization.exclusiveEquipment());

        ConfigurationSection talents = config.createSection("talents");

        for (TalentNode node : specialization.talentTree()) {

            ConfigurationSection nodeSection = talents.createSection(node.id());
            nodeSection.set("display-name", node.displayName());
            nodeSection.set("cost", node.cost());
            nodeSection.set("requires", node.prerequisites());

            ConfigurationSection nodeStats = nodeSection.createSection("stats");
            node.statBonus().forEach(nodeStats::set);

            if (node.grantedSkill() != null) {
                nodeSection.set("skill", node.grantedSkill());
            }
            if (node.grantedTrait() != null) {
                nodeSection.set("trait", node.grantedTrait());
            }
            if (node.grantedEnchantment() != null) {
                nodeSection.set("enchantment", node.grantedEnchantment());
            }
        }

        try {
            folder.mkdirs();
            config.save(new File(folder, specialization.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la especialización " + specialization.id(), e);
        }
    }

}
