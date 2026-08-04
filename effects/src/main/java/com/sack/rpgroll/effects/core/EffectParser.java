package com.sack.rpgroll.effects.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EffectParser implements ContentParser<EffectDefinition> {

    @Override
    public EffectDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String icon = config.getString("icon", "NETHER_STAR");
        String color = config.getString("color", "WHITE");
        String description = config.getString("description", "");

        EffectCategory category = parseEnum(EffectCategory.class, config.getString("category"), EffectCategory.OTHER,
                id, "category");
        EffectRarity rarity = parseEnum(EffectRarity.class, config.getString("rarity"), EffectRarity.COMMON, id,
                "rarity");

        int duration = config.getInt("duration", 100);
        int priority = config.getInt("priority", 0);
        boolean visible = config.getBoolean("visible", true);

        Set<String> tags = new LinkedHashSet<>(config.getStringList("tags"));
        Set<String> conflicts = new LinkedHashSet<>(config.getStringList("conflicts"));

        ConfigurationSection stackingSection = config.getConfigurationSection("stacking");
        EffectStackingMode stackingMode = EffectStackingMode.NONE;
        int maxStacks = 1;
        String upgradeToEffectId = null;

        if (stackingSection != null) {
            stackingMode = parseEnum(EffectStackingMode.class, stackingSection.getString("mode"),
                    EffectStackingMode.NONE, id, "stacking.mode");
            maxStacks = stackingSection.getInt("max-stacks", 1);
            upgradeToEffectId = stackingSection.getString("upgrade-to");
        }

        List<EffectCondition> conditions = parseConditions(config.getMapList("conditions"), id);
        List<EffectComponent> components = parseComponents(config.getMapList("components"), id);

        return new EffectDefinition(id, displayName, icon, color, category, rarity, description, duration, priority,
                visible, conditions, tags, conflicts, stackingMode, maxStacks, upgradeToEffectId, components);
    }

    private List<EffectCondition> parseConditions(List<?> rawConditions, String effectId) {

        List<EffectCondition> conditions = new ArrayList<>();

        for (Object raw : rawConditions) {

            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            EffectConditionType type;

            try {
                type = EffectConditionType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "efecto '" + effectId + "' tiene una condición con type inválido: " + rawType);
            }

            conditions.add(new EffectCondition(type, extractParams(map)));
        }

        return conditions;
    }

    private List<EffectComponent> parseComponents(List<?> rawComponents, String effectId) {

        List<EffectComponent> components = new ArrayList<>();

        for (Object raw : rawComponents) {

            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            EffectComponentType type;

            try {
                type = EffectComponentType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "efecto '" + effectId + "' tiene un componente con type inválido: " + rawType);
            }

            EffectTriggerType trigger = EffectTriggerType.ON_APPLY;
            Object rawTrigger = map.get("trigger");

            if (rawTrigger != null) {
                try {
                    trigger = EffectTriggerType.valueOf(rawTrigger.toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "efecto '" + effectId + "' tiene un componente con trigger inválido: " + rawTrigger);
                }
            }

            Map<String, String> params = extractParams(map);
            params.remove("trigger");

            components.add(new EffectComponent(type, trigger, params));
        }

        return components;
    }

    private Map<String, String> extractParams(Map<?, ?> map) {

        Map<String, String> params = new LinkedHashMap<>();

        for (var entry : map.entrySet()) {

            String key = String.valueOf(entry.getKey());

            if (key.equals("type") || entry.getValue() == null) {
                continue;
            }

            params.put(key, String.valueOf(entry.getValue()));
        }

        return params;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback, String effectId, String field) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("efecto '" + effectId + "' tiene un valor inválido en " + field + ": " + raw);
        }
    }

}
