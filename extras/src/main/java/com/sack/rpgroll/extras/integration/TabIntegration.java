package com.sack.rpgroll.extras.integration;

import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Punto de entrada seguro — comprueba que RPGRoll-TAB esté instalado ANTES de tocar cualquiera de sus clases. */
public final class TabIntegration {

    private TabIntegration() {
    }

    public static void registerIfPresent(Plugin plugin, StatManager statManager, StatEngine statEngine,
            BodyTemperatureEngine temperatureEngine, ConditionRuntime conditionRuntime) {

        if (Bukkit.getPluginManager().getPlugin("RPGRoll-TAB") == null) {
            return;
        }

        try {
            TabIntegrationBridge.register(statManager, statEngine, temperatureEngine, conditionRuntime);
            plugin.getLogger().info("✔ Placeholders registrados en RPGRoll-TAB.");
        } catch (Exception e) {
            plugin.getLogger().warning("✘ No se pudo integrar con RPGRoll-TAB: " + e.getMessage());
        }
    }

}
