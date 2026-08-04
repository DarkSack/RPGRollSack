package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassSpecializationParser implements ContentParser<ClassSpecialization> {

    @Override
    public ClassSpecialization parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String baseClass = config.getString("base-class");
        if (baseClass == null || baseClass.isBlank()) {
            throw new IllegalArgumentException("especialización '" + id + "' sin campo obligatorio 'base-class'");
        }

        String displayName = config.getString("display-name", id);
        AscensionRequirements requirements = AscensionRequirementsParser.parse(
                config.getConfigurationSection("requirements"));

        Map<String, Double> statBonus = parseNumberMap(config.getConfigurationSection("stats"));
        List<String> restrictions = config.getStringList("restrictions");
        List<String> exclusiveEquipment = config.getStringList("exclusive-equipment");

        List<TalentNode> talentTree = parseTalentTree(config.getConfigurationSection("talents"));

        return new ClassSpecialization(id, baseClass.toLowerCase(Locale.ROOT), displayName, requirements, statBonus,
                restrictions, exclusiveEquipment, talentTree);
    }

    private Map<String, Double> parseNumberMap(ConfigurationSection section) {

        Map<String, Double> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            if (section.get(key) instanceof Number number) {
                result.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
            }
        }

        return result;
    }

    private List<TalentNode> parseTalentTree(ConfigurationSection section) {

        List<TalentNode> nodes = new ArrayList<>();

        if (section == null) {
            return nodes;
        }

        for (String nodeId : section.getKeys(false)) {

            ConfigurationSection nodeSection = section.getConfigurationSection(nodeId);
            if (nodeSection == null) {
                continue;
            }

            String displayName = nodeSection.getString("display-name", nodeId);
            int cost = nodeSection.getInt("cost", 1);
            List<String> prerequisites = nodeSection.getStringList("requires");
            Map<String, Double> statBonus = parseNumberMap(nodeSection.getConfigurationSection("stats"));
            String grantedSkill = nodeSection.getString("skill");
            String grantedTrait = nodeSection.getString("trait");
            String grantedEnchantment = nodeSection.getString("enchantment");

            nodes.add(new TalentNode(nodeId, displayName, cost, prerequisites, statBonus, grantedSkill, grantedTrait,
                    grantedEnchantment));
        }

        return nodes;
    }

}
