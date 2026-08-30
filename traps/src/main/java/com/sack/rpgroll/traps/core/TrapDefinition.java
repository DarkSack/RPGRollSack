package com.sack.rpgroll.traps.core;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Define un TIPO de trampa/mecanismo (ej. "landmine", "spike_corridor") —
 * no una ubicación física en el mundo, eso es {@link com.sack.rpgroll.traps.location.PlacedTrap}.
 * Varias ubicaciones pueden compartir el mismo TrapDefinition (mismo
 * comportamiento, distintos puestos).
 *
 * @param triggerParams  config extra específica del trigger (ej. TIMER usa
 *                       "interval-seconds"; PROJECTILE/INTERACTION no
 *                       necesitan nada) — mismo patrón de params libres que
 *                       {@link TrapAction}.
 * @param radius         radio en bloques usado por PRESSURE/MOVEMENT/PROXIMITY/
 *                       LINE_OF_SIGHT para saber si un jugador "está encima".
 * @param charges        -1 = infinitas; en 0 la trampa cae en DEPLETED.
 * @param chain          ids de otras trampas a forzar-disparar en cadena
 *                       cuando esta se dispara (ver TrapEngine#forceTrigger).
 */
public record TrapDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        TrapTrigger trigger,
        Map<String, String> triggerParams,
        double radius,
        List<String> conditions,
        List<TrapAction> actions,
        long cooldownMillis,
        int charges,
        List<String> chain,
        TrapBlockConfig block,
        TrapDisguiseConfig disguise) implements RPGContent {

    public TrapDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(trigger, "trigger no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        icon = icon == null ? Material.TRIPWIRE_HOOK : icon;
        triggerParams = triggerParams == null ? Map.of() : Map.copyOf(triggerParams);
        radius = radius <= 0 ? 1.5 : radius;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        charges = charges == 0 ? -1 : charges;
        chain = chain == null ? List.of() : List.copyOf(chain);
        block = block == null ? TrapBlockConfig.none() : block;
        disguise = disguise == null ? TrapDisguiseConfig.none() : disguise;
    }

    public String triggerParam(String key, String fallback) {
        return triggerParams.getOrDefault(key, fallback);
    }

    public boolean hasInfiniteCharges() {
        return charges < 0;
    }

}
