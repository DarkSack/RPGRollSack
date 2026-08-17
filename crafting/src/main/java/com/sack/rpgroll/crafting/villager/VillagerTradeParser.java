package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VillagerTradeParser implements ContentParser<VillagerTradeDefinition> {

    @Override
    public VillagerTradeDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<RecipeResult> costs = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList("costs")) {
            costs.add(parseResult(raw, id, "costs"));
        }

        RecipeResult result = parseResult(config.getConfigurationSection("result"), id);

        return new VillagerTradeDefinition(
                id,
                config.getString("display-name", id),
                config.getString("icon", "EMERALD"),
                costs,
                result,
                config.getInt("max-uses", 12),
                config.getInt("villager-experience", 1),
                config.getBoolean("rewards-experience", true),
                parseConditions(config.getMapList("conditions"), id),
                config.getDouble("xp-amount", 0),
                config.getString("economy-currency-id"),
                config.getDouble("economy-cost", 0),
                config.getBoolean("quality-enabled", false));
    }

    private RecipeResult parseResult(ConfigurationSection section, String tradeId) {

        if (section == null) {
            throw new IllegalArgumentException("comercio '" + tradeId + "' sin sección obligatoria 'result'");
        }

        RecipeResultType type;
        try {
            type = RecipeResultType.valueOf(section.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("comercio '" + tradeId + "' tiene result.type inválido");
        }

        return new RecipeResult(type, section.getString("value"), section.getInt("amount", 1));
    }

    private RecipeResult parseResult(Map<?, ?> map, String tradeId, String field) {

        RecipeResultType type;
        Object rawType = map.get("type");
        try {
            type = RecipeResultType.valueOf((rawType != null ? rawType.toString() : "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("comercio '" + tradeId + "' tiene un " + field + " con type inválido: " + rawType);
        }

        String value = map.get("value") != null ? map.get("value").toString() : null;
        int amount = map.get("amount") != null ? Integer.parseInt(map.get("amount").toString()) : 1;

        return new RecipeResult(type, value, amount);
    }

    private List<RecipeCondition> parseConditions(List<?> raw, String tradeId) {

        List<RecipeCondition> conditions = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            ConditionType type;
            try {
                type = ConditionType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "comercio '" + tradeId + "' tiene una condición con type inválido: " + map.get("type"));
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int minValue = map.get("min-value") != null ? Integer.parseInt(map.get("min-value").toString()) : 0;

            conditions.add(new RecipeCondition(type, value, minValue));
        }

        return conditions;
    }

}
