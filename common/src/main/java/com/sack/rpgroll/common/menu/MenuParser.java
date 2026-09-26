package com.sack.rpgroll.common.menu;

import com.sack.rpgroll.common.content.ContentParser;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lee un menú YAML. Ver menus/_reference_full_menu.yml en RPGRoll-NPCs para todos los campos. */
public class MenuParser implements ContentParser<MenuDefinition> {

    @Override
    public MenuDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String title = config.getString("title", id);
        int rows = config.getInt("rows", 3);

        List<MenuItem> items = parseItems(config);

        return new MenuDefinition(id, title, rows, items, config.getString("filler"));
    }

    private List<MenuItem> parseItems(YamlConfiguration config) {

        List<MenuItem> items = new ArrayList<>();
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

            List<MenuAction> actions = parseItemActions(raw);

            Object permission = raw.get("permission");

            items.add(new MenuItem(slot, material, displayName, lore, actions,
                    permission == null ? null : permission.toString()));
        }

        return items;
    }

    private List<MenuAction> parseItemActions(Map<?, ?> raw) {
        return raw.get("actions") instanceof List<?> rawActions ? parseActions(rawActions) : List.of();
    }

    /**
     * Una lista de {@code {type, value}}. Las entradas incompletas o de un tipo
     * desconocido se ignoran. La usan también los NPCs para sus acciones.
     */
    public static List<MenuAction> parseActions(List<?> rawActions) {

        List<MenuAction> actions = new ArrayList<>();

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
                MenuAction.ActionType type = MenuAction.ActionType.valueOf(typeObj.toString().toUpperCase());
                actions.add(new MenuAction(type, valueObj.toString()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return actions;
    }

}