package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LuckyParser implements ContentParser<LuckyBlock> {

    @Override
    public LuckyBlock parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String model = config.getString("item.model", "");

        return new LuckyBlock(
                id.trim().toLowerCase(Locale.ROOT),
                config.getString("display-name", id),
                config.getStringList("lore"),
                config.getInt("note", -1),
                config.getString("item.material", "PAPER"),
                model.isBlank() ? null : model.trim(),
                config.getBoolean("item.glow", false),
                outcomes(config.getMapList("outcomes"), id));
    }

    private static List<LuckyOutcome> outcomes(List<Map<?, ?>> raw, String blockId) {

        List<LuckyOutcome> out = new ArrayList<>();

        for (Map<?, ?> map : raw) {

            Object idObj = map.get("id");
            if (idObj == null) {
                throw new IllegalArgumentException("lucky block '" + blockId + "': un resultado no tiene id");
            }
            String id = idObj.toString();

            LuckyOutcome.Luck luck;
            try {
                luck = LuckyOutcome.Luck.valueOf(string(map, "luck", "NEUTRAL").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("lucky block '" + blockId + "', resultado '" + id
                        + "': luck debe ser GOOD, NEUTRAL o BAD");
            }

            List<LuckyAction> actions = new ArrayList<>();
            if (map.get("actions") instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> actionMap) {
                        actions.add(action(actionMap, blockId, id));
                    }
                }
            }

            out.add(new LuckyOutcome(id, Double.parseDouble(string(map, "weight", "1")), luck,
                    string(map, "message", null), Boolean.parseBoolean(string(map, "announce", "false")), actions));
        }

        return out;
    }

    private static LuckyAction action(Map<?, ?> map, String blockId, String outcomeId) {

        Object type = map.get("type");
        if (type == null) {
            throw new IllegalArgumentException("lucky block '" + blockId + "', resultado '" + outcomeId
                    + "': una acción no tiene type");
        }

        LuckyAction.Type parsed;
        try {
            parsed = LuckyAction.parseType(type.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("lucky block '" + blockId + "', resultado '" + outcomeId
                    + "': tipo de acción desconocido '" + type + "'");
        }

        Map<String, String> params = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null && value != null && !"type".equals(key.toString())) {
                params.put(key.toString().toLowerCase(Locale.ROOT), value.toString());
            }
        });

        return new LuckyAction(parsed, params);
    }

    private static String string(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value.toString();
    }

}
