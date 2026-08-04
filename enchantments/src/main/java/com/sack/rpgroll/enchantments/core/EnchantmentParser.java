package com.sack.rpgroll.enchantments.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Convierte un YAML de enchantments/ en un {@link CustomEnchantment}. Todos
 * los bloques ("categories", "trigger", "conditions", "levels", "effects")
 * son opcionales salvo "id" — cada uno cae a un default seguro si falta.
 */
public class EnchantmentParser implements ContentParser<CustomEnchantment> {

    @Override
    public CustomEnchantment parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        Rarity rarity = parseRarity(config.getString("rarity"));
        int maxLevel = config.getInt("max-level", 1);

        Set<EnchantCategory> categories = parseCategories(config.getStringList("categories"));
        List<Material> allowedItems = parseMaterials(config.getStringList("allowed-items"));
        List<String> conflicts = upperCaseAll(config.getStringList("conflicts"));

        Set<Trigger> triggers = parseTriggers(config.getStringList("trigger"));
        List<String> conditions = config.getStringList("conditions");
        double chance = parseChance(config.get("chance"));

        Map<Integer, Map<String, Double>> levelData = parseLevels(config.getConfigurationSection("levels"));
        List<EnchantEffect> effects = parseEffects(config.getList("effects"));

        return new CustomEnchantment(id, displayName, rarity, categories, allowedItems, maxLevel,
                levelData, conflicts, triggers, conditions, chance, effects);
    }

    private Rarity parseRarity(String raw) {

        if (raw == null || raw.isBlank()) {
            return Rarity.COMMON;
        }

        try {
            return Rarity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Rarity.COMMON;
        }
    }

    private Set<EnchantCategory> parseCategories(List<String> raw) {

        Set<EnchantCategory> categories = new LinkedHashSet<>();

        for (String value : raw) {
            EnchantCategory category = EnchantCategory.fromString(value);
            if (category != null) {
                categories.add(category);
            }
        }

        return categories;
    }

    private List<Material> parseMaterials(List<String> raw) {

        List<Material> materials = new ArrayList<>();

        for (String value : raw) {
            try {
                materials.add(Material.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return materials;
    }

    private List<String> upperCaseAll(List<String> raw) {

        List<String> result = new ArrayList<>();

        for (String value : raw) {
            result.add(value.trim().toUpperCase(Locale.ROOT));
        }

        return result;
    }

    private Set<Trigger> parseTriggers(List<String> raw) {

        Set<Trigger> triggers = EnumSet.noneOf(Trigger.class);

        for (String value : raw) {
            try {
                triggers.add(Trigger.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return triggers;
    }

    private double parseChance(Object raw) {

        if (raw == null) {
            return 100.0;
        }

        String value = raw.toString().trim();

        try {
            if (value.endsWith("%")) {
                return Double.parseDouble(value.substring(0, value.length() - 1));
            }
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 100.0;
        }
    }

    private Map<Integer, Map<String, Double>> parseLevels(ConfigurationSection section) {

        Map<Integer, Map<String, Double>> levels = new HashMap<>();

        if (section == null) {
            return levels;
        }

        for (String levelKey : section.getKeys(false)) {

            int level;
            try {
                level = Integer.parseInt(levelKey.trim());
            } catch (NumberFormatException e) {
                continue;
            }

            ConfigurationSection levelSection = section.getConfigurationSection(levelKey);
            if (levelSection == null) {
                continue;
            }

            Map<String, Double> data = new HashMap<>();

            for (String dataKey : levelSection.getKeys(false)) {
                if (levelSection.get(dataKey) instanceof Number number) {
                    data.put(dataKey, number.doubleValue());
                }
            }

            levels.put(level, data);
        }

        return levels;
    }

    private List<EnchantEffect> parseEffects(List<?> raw) {

        List<EnchantEffect> effects = new ArrayList<>();

        if (raw == null) {
            return effects;
        }

        for (Object entry : raw) {

            if (entry instanceof String simple) {
                parseSimpleEffect(simple).ifPresent(effects::add);
                continue;
            }

            if (entry instanceof Map<?, ?> map) {
                parseDetailedEffect(map).ifPresent(effects::add);
            }
        }

        return effects;
    }

    private Optional<EnchantEffect> parseSimpleEffect(String raw) {

        try {
            return Optional.of(
                    new EnchantEffect(EnchantEffectType.valueOf(raw.trim().toUpperCase(Locale.ROOT)), Map.of()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<EnchantEffect> parseDetailedEffect(Map<?, ?> map) {

        Object typeObj = map.get("type");
        if (typeObj == null) {
            return Optional.empty();
        }

        EnchantEffectType type;
        try {
            type = EnchantEffectType.valueOf(typeObj.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        Map<String, String> params = new HashMap<>();

        for (var mapEntry : map.entrySet()) {

            String key = mapEntry.getKey().toString();

            if (key.equals("type") || mapEntry.getValue() == null) {
                continue;
            }

            params.put(key, mapEntry.getValue().toString());
        }

        return Optional.of(new EnchantEffect(type, params));
    }

}
