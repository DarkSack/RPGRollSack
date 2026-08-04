package com.sack.rpgroll.dungeons.core;

import java.util.Map;

/**
 * Una acción ejecutable en respuesta a un {@link DungeonTrigger}, al
 * activarse un mecanismo de puzzle, o dentro de una recompensa. Tipo
 * libre — el motor trae tipos base (ver
 * {@link com.sack.rpgroll.dungeons.registry.BuiltinDungeonActions}),
 * extensible vía {@code ActionRegistry#register(String, DungeonActionHandler)}.
 */
public record DungeonAction(String type, Map<String, String> params) {

    public DungeonAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
