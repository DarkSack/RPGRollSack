package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.condition.ItemConditionContext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Rutas de variable adicionales para condiciones (ej. "guild.rank == 3").
 * El evaluador trae las rutas base (player.*, target.*, world, weather);
 * un addon puede sumar las suyas — {@code api.items().registerCondition(...)}.
 */
public class ConditionRegistry {

    private final Map<String, Function<ItemConditionContext, Object>> variables = new HashMap<>();

    public void registerVariable(String path, Function<ItemConditionContext, Object> resolver) {
        variables.put(path.trim().toLowerCase(Locale.ROOT), resolver);
    }

    public Object resolve(String path, ItemConditionContext context) {
        Function<ItemConditionContext, Object> resolver = variables.get(path.trim().toLowerCase(Locale.ROOT));
        return resolver != null ? resolver.apply(context) : null;
    }

}
