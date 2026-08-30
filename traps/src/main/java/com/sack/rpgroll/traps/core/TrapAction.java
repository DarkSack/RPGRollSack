package com.sack.rpgroll.traps.core;

import java.util.Map;

/**
 * Una acción ejecutable al disparar una trampa. Tipo libre — el motor trae
 * los tipos base (ver {@code BuiltinTrapActions}), extensible vía
 * {@code TrapActionRegistry#register(String, TrapActionHandler)}. Mismo
 * espíritu que {@code MobAction} de RPGRoll-Mobs.
 */
public record TrapAction(String type, Map<String, String> params) {

    public TrapAction {
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
