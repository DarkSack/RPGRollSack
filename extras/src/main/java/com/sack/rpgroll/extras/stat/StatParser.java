package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.extras.action.ActionParsingUtil;
import com.sack.rpgroll.extras.action.ExtrasAction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatParser implements ContentParser<StatDefinition> {

    private static final int DEFAULT_INTERVAL_TICKS = 20;

    @Override
    public StatDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        boolean enabled = config.getBoolean("enabled", true);
        double max = config.getDouble("max", 100);
        double start = config.getDouble("start", max);

        DecayRule decay = parseDecay(config.getConfigurationSection("decay"));
        RegenerationConfig regeneration = parseRegeneration(config, id);
        Map<String, Double> consumption = parseConsumption(config.getConfigurationSection("consumption"));
        List<StatThreshold> thresholds = parseThresholds(config.getMapList("thresholds"), id);

        return new StatDefinition(id, enabled, max, start, decay, regeneration, consumption, thresholds);
    }

    private DecayRule parseDecay(ConfigurationSection section) {

        if (section == null) {
            return null;
        }

        double amount = section.getDouble("amount", 1);
        int interval = section.getInt("interval", DEFAULT_INTERVAL_TICKS);

        return new DecayRule(amount, interval);
    }

    /** Detecta si "regeneration" es la forma corta {amount,interval} o la lista condicional (sección 4). */
    private RegenerationConfig parseRegeneration(YamlConfiguration config, String statId) {

        Object raw = config.get("regeneration");

        if (raw == null) {
            return null;
        }

        if (raw instanceof List<?>) {

            List<RateRule> rules = new ArrayList<>();

            for (Map<?, ?> entry : config.getMapList("regeneration")) {

                Object conditionObj = entry.get("condition");
                Object amountObj = entry.get("amount");

                if (amountObj == null) {
                    continue;
                }

                rules.add(new RateRule(
                        conditionObj == null ? null : conditionObj.toString(),
                        Double.parseDouble(amountObj.toString())));
            }

            int interval = config.getInt("regeneration-interval", DEFAULT_INTERVAL_TICKS);
            return new RegenerationConfig(rules, interval);
        }

        ConfigurationSection section = config.getConfigurationSection("regeneration");

        if (section == null) {
            throw new IllegalArgumentException("stat '" + statId + "': 'regeneration' con formato inválido");
        }

        double amount = section.getDouble("amount", 0);
        int interval = section.getInt("interval", DEFAULT_INTERVAL_TICKS);

        return new RegenerationConfig(List.of(RateRule.unconditional(amount)), interval);
    }

    private Map<String, Double> parseConsumption(ConfigurationSection section) {

        Map<String, Double> consumption = new LinkedHashMap<>();

        if (section == null) {
            return consumption;
        }

        for (String key : section.getKeys(false)) {
            consumption.put(key.toLowerCase(java.util.Locale.ROOT), section.getDouble(key));
        }

        return consumption;
    }

    private List<StatThreshold> parseThresholds(List<Map<?, ?>> raw, String statId) {

        List<StatThreshold> thresholds = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            Object conditionObj = entry.get("condition");
            if (conditionObj == null) {
                throw new IllegalArgumentException("stat '" + statId + "': threshold sin 'condition'");
            }

            List<PotionSpec> potions = parsePotions(entry.get("potions"));

            List<Map<?, ?>> rawActions = new ArrayList<>();
            if (entry.get("actions") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        rawActions.add(map);
                    }
                }
            }

            List<ExtrasAction> actions = ActionParsingUtil.parseActions(rawActions);

            List<String> applyConditions = entry.get("apply-conditions") instanceof List<?> conditionsList
                    ? conditionsList.stream().map(Object::toString).toList()
                    : List.of();

            thresholds.add(new StatThreshold(conditionObj.toString(), potions, actions, applyConditions));
        }

        return thresholds;
    }

    private List<PotionSpec> parsePotions(Object raw) {

        List<PotionSpec> potions = new ArrayList<>();

        if (!(raw instanceof List<?> list)) {
            return potions;
        }

        for (Object item : list) {

            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Object typeObj = map.get("type");
            if (typeObj == null) {
                continue;
            }

            int amplifier = map.get("amplifier") != null ? Integer.parseInt(map.get("amplifier").toString()) : 0;
            potions.add(new PotionSpec(typeObj.toString(), amplifier));
        }

        return potions;
    }

}
