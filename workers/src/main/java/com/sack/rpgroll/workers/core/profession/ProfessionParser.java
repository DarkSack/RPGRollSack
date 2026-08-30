package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.common.reskin.EntityReskinYaml;
import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.ai.AiCondition;
import com.sack.rpgroll.workers.core.economy.WageType;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfessionParser implements ContentParser<Profession> {

    @Override
    public Profession parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Set<String> skillIds = new HashSet<>();
        for (String skillId : config.getStringList("skills")) {
            skillIds.add(skillId.trim().toLowerCase(Locale.ROOT));
        }

        return new Profession(
                id,
                config.getString("display-name", id),
                config.getString("icon", "VILLAGER_SPAWN_EGG"),
                config.getString("description", ""),
                config.getString("entity-type", "VILLAGER"),
                skillIds,
                parseRules(config),
                config.getString("schedule"),
                config.getDouble("wage-amount", 0),
                parseWageType(config.getString("wage-type")),
                config.getString("tool-material"),
                EntityReskinYaml.parse(config));
    }

    private WageType parseWageType(String raw) {

        if (raw == null || raw.isBlank()) {
            return WageType.PER_TASK;
        }

        try {
            return WageType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WageType.PER_TASK;
        }
    }

    private List<AiRule> parseRules(YamlConfiguration config) {

        List<AiRule> rules = new ArrayList<>();
        List<?> rawList = config.getList("ai-rules");

        if (rawList == null) {
            return rules;
        }

        for (Object rawEntry : rawList) {

            if (!(rawEntry instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }

            AiCondition condition;
            AiAction action;

            try {
                condition = AiCondition.valueOf(String.valueOf(rawMap.get("condition")).toUpperCase(Locale.ROOT));
                action = AiAction.valueOf(String.valueOf(rawMap.get("action")).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            int priority = rawMap.get("priority") instanceof Number number ? number.intValue() : 100;

            rules.add(new AiRule(condition, action, priority));
        }

        return rules;
    }

}
