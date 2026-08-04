package com.sack.rpgroll.quests.registry;

import com.sack.rpgroll.quests.condition.QuestConditionContext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Registro de rutas de variable adicionales para condiciones de stage
 * (ej. "player.level &gt;= 10"). El evaluador trae las rutas base
 * (player.*, race, class, world, weather, region); un addon puede sumar
 * las suyas (ej. "guild.rank") vía {@link #registerVariable(String, Function)}
 * — esa es la vía de extensión para "registerCondition" de la API.
 */
public class ConditionRegistry {

    private final Map<String, Function<QuestConditionContext, Object>> variables = new HashMap<>();

    public void registerVariable(String path, Function<QuestConditionContext, Object> resolver) {
        variables.put(path.trim().toLowerCase(Locale.ROOT), resolver);
    }

    public Object resolve(String path, QuestConditionContext context) {

        Function<QuestConditionContext, Object> resolver = variables.get(path.trim().toLowerCase(Locale.ROOT));

        return resolver != null ? resolver.apply(context) : null;
    }

}
