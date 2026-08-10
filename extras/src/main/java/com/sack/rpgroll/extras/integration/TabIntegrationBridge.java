package com.sack.rpgroll.extras.integration;

import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;
import com.sack.rpgroll.tab.api.TABAPI;

/**
 * Único punto de contacto con las clases reales de RPGRoll-TAB — aislado
 * para que la JVM nunca las resuelva si TAB no está instalado (igual
 * patrón que {@code PlaceholderApiBridge} de RPGRoll-TAB). Registra cada
 * stat como {@code {extras_<id>}} y la temperatura corporal como
 * {@code {extras_body_temperature}} vía {@code TABPlaceholderRegistry} —
 * sección 13.
 */
final class TabIntegrationBridge {

    private TabIntegrationBridge() {
    }

    static void register(StatManager statManager, StatEngine statEngine, BodyTemperatureEngine temperatureEngine,
            ConditionRuntime conditionRuntime) {

        var placeholders = TABAPI.get().placeholders();

        for (var stat : statManager.getAll()) {
            placeholders.register("extras_" + stat.id(), player -> String.valueOf(Math.round(statEngine.get(player, stat.id()))));
            placeholders.register("extras_" + stat.id() + "_max", player -> String.valueOf(Math.round(stat.max())));
        }

        placeholders.register("extras_body_temperature",
                player -> String.format(java.util.Locale.ROOT, "%.1f", temperatureEngine.bodyTemperature(player)));
        placeholders.register("extras_temperature_state", temperatureEngine::currentStateId);
        placeholders.register("extras_conditions",
                player -> String.join(", ", conditionRuntime.activeConditions(player)));
    }

}
