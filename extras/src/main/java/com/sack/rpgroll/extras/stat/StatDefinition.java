package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;

/**
 * Un "need"/"attribute" genérico (sed, stamina, fatiga, oxígeno, estrés,
 * sanity, mana, lo que sea) — el mismo motor sirve para cualquiera, el
 * administrador define el comportamiento entero por YAML (sección 27).
 * <p>
 * {@code restore}: cuánto sube al consumir cada ítem (clave = material en
 * minúsculas, o {@code water_bottle} para la botella de agua). Sin esto la
 * sed no tenía forma de recuperarse.
 * {@code resetOnDeath}: al reaparecer vuelve a {@code start}.
 */
public record StatDefinition(
        String id,
        boolean enabled,
        double max,
        double start,
        DecayRule decay,
        RegenerationConfig regeneration,
        Map<String, Double> consumption,
        List<StatThreshold> thresholds,
        Map<String, Double> restore,
        boolean resetOnDeath) implements RPGContent {

    public StatDefinition {
        consumption = consumption == null ? Map.of() : Map.copyOf(consumption);
        thresholds = thresholds == null ? List.of() : List.copyOf(thresholds);
        restore = restore == null ? Map.of() : Map.copyOf(restore);
    }

    public StatDefinition(String id, boolean enabled, double max, double start, DecayRule decay,
            RegenerationConfig regeneration, Map<String, Double> consumption, List<StatThreshold> thresholds) {
        this(id, enabled, max, start, decay, regeneration, consumption, thresholds, Map.of(), true);
    }
}
