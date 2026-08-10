package com.sack.rpgroll.extras.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parseo compartido de listas de {@code actions:} — mismo formato {type, value} que Crates/Mobs/Items. */
public final class ActionParsingUtil {

    private ActionParsingUtil() {
    }

    public static List<ExtrasAction> parseActions(List<Map<?, ?>> raw) {

        List<ExtrasAction> actions = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            Object typeObj = entry.get("type");
            Object valueObj = entry.get("value");

            if (typeObj == null || valueObj == null) {
                continue;
            }

            try {
                ExtrasActionType type = ExtrasActionType.valueOf(typeObj.toString().trim().toUpperCase());
                actions.add(new ExtrasAction(type, valueObj.toString()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return actions;
    }

}
