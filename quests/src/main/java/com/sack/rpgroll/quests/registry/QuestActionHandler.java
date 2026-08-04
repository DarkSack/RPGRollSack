package com.sack.rpgroll.quests.registry;

import com.sack.rpgroll.quests.core.QuestAction;

/**
 * Ejecuta un tipo concreto de {@link QuestAction}. El motor trae handlers
 * base ya registrados (ver {@link ActionRegistry}); los addons pueden sumar
 * los propios llamando a {@code ActionRegistry#register(String, QuestActionHandler)}
 * — esa es la vía de extensión para "registerEvent"/"registerReward" de la API.
 */
@FunctionalInterface
public interface QuestActionHandler {
    void execute(QuestAction action, QuestActionContext context);
}
