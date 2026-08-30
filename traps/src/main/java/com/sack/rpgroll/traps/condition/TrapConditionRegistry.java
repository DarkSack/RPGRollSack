package com.sack.rpgroll.traps.condition;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Rutas de variable adicionales para condiciones de trampa (ej.
 * "guild.rank == 3" si otro addon quisiera sumar la suya). Extensible por
 * código, no expuesto todavía en una API pública — no hay otro addon que
 * dependa de RPGRoll-Traps en v1.
 */
public class TrapConditionRegistry {

    private final Map<String, Function<TrapConditionContext, Object>> variables = new HashMap<>();

    public void registerVariable(String path, Function<TrapConditionContext, Object> resolver) {
        variables.put(path.trim().toLowerCase(Locale.ROOT), resolver);
    }

    public Object resolve(String path, TrapConditionContext context) {
        Function<TrapConditionContext, Object> resolver = variables.get(path.trim().toLowerCase(Locale.ROOT));
        return resolver != null ? resolver.apply(context) : null;
    }

}
