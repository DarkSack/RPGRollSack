package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;

/**
 * Un "need"/"attribute" genérico (sed, stamina, fatiga, oxígeno, estrés,
 * sanity, mana, lo que sea) — el mismo motor sirve para cualquiera, el
 * administrador define el comportamiento entero por YAML (sección 27).
 */
public record StatDefinition(
        String id,
        boolean enabled,
        double max,
        double start,
        DecayRule decay,
        RegenerationConfig regeneration,
        Map<String, Double> consumption,
        List<StatThreshold> thresholds) implements RPGContent {
}
