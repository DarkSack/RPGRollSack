package com.sack.rpgroll.quests.core;

import java.util.List;

/**
 * Recompensas otorgadas al completar una quest. Los cinco campos fijos
 * cubren el caso común (dinero, experiencia, items, comandos, encadenar
 * otra quest); {@code extra} permite sumar cualquier acción registrada en
 * {@link com.sack.rpgroll.quests.registry.ActionRegistry} — esa es la vía
 * de extensión para "registerReward" de la API.
 */
public record QuestRewards(
        double money,
        int experience,
        List<ItemRequirement> items,
        List<String> commands,
        List<String> quests,
        List<QuestAction> extra) {

    public QuestRewards {
        items = items == null ? List.of() : List.copyOf(items);
        commands = commands == null ? List.of() : List.copyOf(commands);
        quests = quests == null ? List.of() : List.copyOf(quests);
        extra = extra == null ? List.of() : List.copyOf(extra);
    }

    public static QuestRewards none() {
        return new QuestRewards(0, 0, List.of(), List.of(), List.of(), List.of());
    }

}
