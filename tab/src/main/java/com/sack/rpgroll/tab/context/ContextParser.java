package com.sack.rpgroll.tab.context;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextParser implements ContentParser<ContextDefinition> {

    @Override
    public ContextDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        int priority = config.getInt("priority", 0);
        List<ContextCondition> conditions = parseConditions(config.getMapList("conditions"), id);

        return new ContextDefinition(
                id,
                priority,
                conditions,
                config.getString("profile"),
                config.getString("tablist"),
                config.getString("scoreboard"),
                config.getString("nametag"),
                config.getString("belowname"),
                config.getString("bossbar"),
                config.getString("layout"));
    }

    private List<ContextCondition> parseConditions(List<Map<?, ?>> raw, String contextId) {

        List<ContextCondition> conditions = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            Object typeObj = entry.get("type");
            if (typeObj == null) {
                continue;
            }

            ContextConditionType type;
            try {
                type = ContextConditionType.valueOf(typeObj.toString().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "contexto '" + contextId + "': tipo de condición inválido: " + typeObj);
            }

            if (type == ContextConditionType.PLACEHOLDER) {

                Object placeholderObj = entry.get("placeholder");
                if (placeholderObj == null) {
                    throw new IllegalArgumentException(
                            "contexto '" + contextId + "': condición 'placeholder' sin campo 'placeholder'");
                }

                ConditionOperator operator = ConditionOperator.EQUALS;
                Object operatorObj = entry.get("operator");
                if (operatorObj != null) {
                    try {
                        operator = ConditionOperator.valueOf(operatorObj.toString().trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "contexto '" + contextId + "': operador inválido: " + operatorObj);
                    }
                }

                Object valueObj = entry.get("value");
                conditions.add(ContextCondition.placeholderCondition(
                        placeholderObj.toString(), operator, valueObj == null ? null : valueObj.toString()));

            } else {

                Object valueObj = entry.get("value");
                if (valueObj == null) {
                    throw new IllegalArgumentException(
                            "contexto '" + contextId + "': condición '" + type + "' sin campo 'value'");
                }

                conditions.add(ContextCondition.nativeCondition(type, valueObj.toString()));
            }
        }

        return conditions;
    }

}
