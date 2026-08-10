package com.sack.rpgroll.tab.sorting;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SortingParser implements ContentParser<SortingDefinition> {

    private static final List<String> DEFAULT_PRIORITY = List.of("permission", "name");

    @Override
    public SortingDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<Map<?, ?>> explicitRules = config.getMapList("rules");

        List<SortRule> rules = explicitRules.isEmpty()
                ? parsePriorityShorthand(config.getStringList("priority"))
                : parseExplicitRules(explicitRules, id);

        return new SortingDefinition(id, rules);
    }

    /** Forma corta: {@code priority: [permission, level, name]} — un campo por línea, orden natural por defecto. */
    private List<SortRule> parsePriorityShorthand(List<String> priority) {

        List<String> effective = priority.isEmpty() ? DEFAULT_PRIORITY : priority;
        List<SortRule> rules = new ArrayList<>();

        for (String field : effective) {

            String normalized = field.trim().toLowerCase(java.util.Locale.ROOT);

            if (normalized.equals("name")) {
                rules.add(SortRule.ofNative("name", SortOrder.ASC));
            } else if (isNativeField(normalized)) {
                rules.add(SortRule.ofNative(normalized, SortOrder.DESC));
            } else {
                // Cualquier otra palabra (level, class, guild, job, ...) se interpreta
                // como el nombre de un placeholder ya registrado por otro addon.
                rules.add(SortRule.ofPlaceholder("{" + normalized + "}", SortOrder.DESC, true));
            }
        }

        return rules;
    }

    private List<SortRule> parseExplicitRules(List<Map<?, ?>> raw, String sortingId) {

        List<SortRule> rules = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            SortOrder order = parseOrder(entry.get("order"));

            Object placeholderObj = entry.get("placeholder");
            if (placeholderObj != null) {
                boolean numeric = entry.get("numeric") != null && Boolean.parseBoolean(entry.get("numeric").toString());
                rules.add(SortRule.ofPlaceholder(placeholderObj.toString(), order, numeric));
                continue;
            }

            Object fieldObj = entry.get("field");
            if (fieldObj == null) {
                throw new IllegalArgumentException(
                        "sorting '" + sortingId + "': cada regla necesita 'field' o 'placeholder'");
            }

            String field = fieldObj.toString().trim().toLowerCase(java.util.Locale.ROOT);

            if (field.equals("permission")) {
                List<String> values = entry.get("values") instanceof List<?> rawValues
                        ? rawValues.stream().map(Object::toString).toList()
                        : List.of();
                rules.add(SortRule.ofNativePermission(values, order));
            } else {
                rules.add(SortRule.ofNative(field, order));
            }
        }

        return rules;
    }

    private SortOrder parseOrder(Object raw) {

        if (raw == null) {
            return SortOrder.DESC;
        }

        try {
            return SortOrder.valueOf(raw.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SortOrder.DESC;
        }
    }

    private boolean isNativeField(String field) {
        return field.equals("ping") || field.equals("world") || field.equals("online-time")
                || field.equals("permission");
    }

}
