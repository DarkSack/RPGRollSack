package com.sack.rpgroll.quests.core;

import java.util.Map;

/**
 * Una acción ejecutable dentro de un evento de quest/stage (on-start,
 * on-complete, etc.) o como recompensa "extra". El tipo es texto libre, no
 * un enum cerrado — el motor conoce un set de tipos base (MESSAGE, SOUND,
 * COMMAND, GIVE_EXP, etc.) vía {@link com.sack.rpgroll.quests.registry.ActionRegistry},
 * pero cualquier addon puede registrar tipos nuevos ahí sin tocar este record.
 */
public record QuestAction(String type, Map<String, String> params) {

    public QuestAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
