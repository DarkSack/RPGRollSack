package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.traps.core.TrapAction;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Define un TIPO de torreta reusable — no una ubicación física, eso es
 * {@link PlacedTurret}. A diferencia de {@code TrapDefinition}, una torreta
 * no reacciona a un evento puntual: reevalúa su objetivo continuamente en
 * el tick del {@link TurretEngine} mientras esté ARMED.
 * <p>
 * {@code impact} es la acción que se ejecuta contra el objetivo en cada
 * disparo — reusa el mismo {@link TrapAction} (tipo+params) que las
 * trampas, ejecutado a través del mismo {@code TrapActionRegistry}: por
 * default es {@code CUSTOM_PROJECTILE} con {@code projectile-type: ARROW}
 * (una flecha, igual que antes), pero puede ser cualquier acción registrada
 * (otro proyectil, SONIC_BOOM, EFFECT, DAMAGE...) — la misma lista de
 * acciones que ya puede usar cualquier trampa custom.
 *
 * <p>
 * Visualmente una torreta son dos piezas: {@code baseBlock}, un bloque
 * entero que se coloca en el mundo (bloque de diamante, sculk sensor...), y
 * {@code model}, un ítem que flota encima. El ítem gira solo mientras no hay
 * objetivo y se orienta hacia él cuando lo detecta, así se nota a simple
 * vista si la torreta está buscando o ya apuntando.
 *
 * @param customModelData null = sin CMD (usa el material tal cual).
 * @param baseBlock       bloque que se coloca en el suelo; null = no coloca ninguno.
 * @param hoverHeight     altura del ítem por encima del bloque, en bloques.
 * @param spinDegreesPerTick giro en reposo; 0 = quieto.
 */
public record TurretDefinition(
        String id,
        String displayName,
        String description,
        double radius,
        boolean targetPlayers,
        boolean targetHostileMobs,
        long fireIntervalTicks,
        List<String> conditions,
        TrapAction impact,
        Material model,
        Integer customModelData,
        Material baseBlock,
        double hoverHeight,
        double spinDegreesPerTick) implements RPGContent {

    private static final TrapAction DEFAULT_IMPACT = new TrapAction("CUSTOM_PROJECTILE",
            Map.of("projectile-type", "ARROW"));

    public TurretDefinition {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        radius = radius <= 0 ? 12.0 : radius;
        fireIntervalTicks = fireIntervalTicks <= 0 ? 20 : fireIntervalTicks;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        impact = impact == null ? DEFAULT_IMPACT : impact;
        model = model == null ? Material.CROSSBOW : model;
        // 0 se respeta (quieto a propósito); solo un valor negativo o sin
        // definir cae al default.
        hoverHeight = hoverHeight <= 0 ? 1.1 : hoverHeight;
        spinDegreesPerTick = spinDegreesPerTick < 0 ? 4.0 : spinDegreesPerTick;
    }

}
