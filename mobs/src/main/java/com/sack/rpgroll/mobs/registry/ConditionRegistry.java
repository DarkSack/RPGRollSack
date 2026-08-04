package com.sack.rpgroll.mobs.registry;

import com.sack.rpgroll.mobs.condition.MobConditionContext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Rutas de variable adicionales para condiciones (ej. "guild.rank == 3").
 * Un addon puede sumar las suyas — {@code api.mobs().registerCondition(...)}.
 */
public class ConditionRegistry {

    private final Map<String, Function<MobConditionContext, Object>> variables = new HashMap<>();

    public void registerVariable(String path, Function<MobConditionContext, Object> resolver) {
        variables.put(path.trim().toLowerCase(Locale.ROOT), resolver);
    }

    public Object resolve(String path, MobConditionContext context) {
        Function<MobConditionContext, Object> resolver = variables.get(path.trim().toLowerCase(Locale.ROOT));
        return resolver != null ? resolver.apply(context) : null;
    }

}
