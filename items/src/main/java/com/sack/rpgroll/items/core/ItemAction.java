package com.sack.rpgroll.items.core;

import java.util.Map;

/**
 * Una acción ejecutable en respuesta a un trigger de ítem, o dentro de una
 * habilidad. Tipo libre — el motor trae tipos base (ver
 * {@link com.sack.rpgroll.items.registry.BuiltinItemActions}), extensible
 * vía {@code ActionRegistry#register(String, ItemActionHandler)}.
 */
public record ItemAction(String type, Map<String, String> params) {

    public ItemAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
