package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.content.ContentParser;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcMenuParser implements ContentParser<NpcMenuDefinition> {

    @Override
    public NpcMenuDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String title = config.getString("title", id);
        int rows = config.getInt("rows", 3);

        List<NpcMenuItem> items = parseItems(config);

        return new NpcMenuDefinition(id, title, rows, items);
    }

    private List<NpcMenuItem> parseItems(YamlConfiguration config) {

        List<NpcMenuItem> items = new ArrayList<>();
        List<Map<?, ?>> rawItems = config.getMapList("items");

        for (Map<?, ?> raw : rawItems) {

            Object slotObj = raw.get("slot");
            Object materialObj = raw.get("material");

            if (slotObj == null || materialObj == null) {
                continue;
            }

            int slot = Integer.parseInt(slotObj.toString());
            String material = materialObj.toString();
            String displayName = raw.get("name") != null ? raw.get("name").toString() : "";

            @SuppressWarnings("unchecked")
            List<String> lore = raw.get("lore") instanceof List<?> rawLore
                    ? rawLore.stream().map(Object::toString).toList()
                    : List.of();

            List<NpcAction> actions = parseItemActions(raw);

            items.add(new NpcMenuItem(slot, material, displayName, lore, actions));
        }

        return items;
    }

    @SuppressWarnings("unchecked")
    private List<NpcAction> parseItemActions(Map<?, ?> raw) {

        List<NpcAction> actions = new ArrayList<>();

        if (!(raw.get("actions") instanceof List<?> rawActions)) {
            return actions;
        }

        for (Object obj : rawActions) {

            if (!(obj instanceof Map<?, ?> actionMap)) {
                continue;
            }

            Object typeObj = actionMap.get("type");
            Object valueObj = actionMap.get("value");

            if (typeObj == null || valueObj == null) {
                continue;
            }

            try {
                NpcAction.NpcActionType type = NpcAction.NpcActionType.valueOf(typeObj.toString().toUpperCase());
                actions.add(new NpcAction(type, valueObj.toString()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return actions;
    }

}