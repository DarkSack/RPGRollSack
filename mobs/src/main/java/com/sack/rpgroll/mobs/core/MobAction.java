package com.sack.rpgroll.mobs.core;

import java.util.Map;

/**
 * Una acción ejecutable en respuesta a un trigger o dentro de una skill.
 * Tipo libre — el motor trae tipos base (ver
 * {@link com.sack.rpgroll.mobs.registry.BuiltinMobActions}), extensible
 * vía {@code ActionRegistry#register(String, MobActionHandler)}.
 */
public record MobAction(String type, Map<String, String> params) {

    public MobAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
