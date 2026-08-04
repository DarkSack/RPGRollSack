package com.sack.rpgroll.effects.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Un efecto de estado completo — buff, debuff, aura, maldición, bendición,
 * etc. No es un simple {@code PotionEffect}: combina una lista de
 * {@link EffectComponent} (lo que hace) con una lista de
 * {@link EffectCondition} (si se puede aplicar) y reglas de acumulación
 * ({@link EffectStackingMode}), conflictos e inmunidades por tag.
 *
 * @param durationTicks duración base en ticks (0 = permanente hasta que se remueva a mano)
 * @param priority      a mayor prioridad, antes se evalúa/se muestra en HUD frente a otros efectos activos
 * @param conditions    requisitos para poder aplicarse — se evalúan una sola vez, al aplicar
 * @param tags          etiquetas libres (ej. "fire", "curse", "debuff") usadas por conflictos/inmunidades
 * @param conflicts     ids de otros efectos que se remueven al aplicar este (y viceversa)
 * @param maxStacks     tope de stacks para STACK_REFRESH/STACK_UPGRADE (ignorado en NONE/INDEPENDENT)
 * @param upgradeToEffectId id del efecto que reemplaza a este al llegar a maxStacks (solo modo UPGRADE)
 * @param components    qué hace el efecto mientras está activo
 */
public record EffectDefinition(
        String id,
        String displayName,
        String icon,
        String color,
        EffectCategory category,
        EffectRarity rarity,
        String description,
        int durationTicks,
        int priority,
        boolean visible,
        List<EffectCondition> conditions,
        Set<String> tags,
        Set<String> conflicts,
        EffectStackingMode stackingMode,
        int maxStacks,
        String upgradeToEffectId,
        List<EffectComponent> components) implements RPGContent {

    public EffectDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "NETHER_STAR" : icon;
        color = color == null || color.isBlank() ? "WHITE" : color;
        category = category == null ? EffectCategory.OTHER : category;
        rarity = rarity == null ? EffectRarity.COMMON : rarity;
        description = description == null ? "" : description;
        durationTicks = Math.max(0, durationTicks);
        maxStacks = Math.max(1, maxStacks);
        stackingMode = stackingMode == null ? EffectStackingMode.NONE : stackingMode;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        conflicts = conflicts == null ? Set.of() : Set.copyOf(conflicts);
        components = components == null ? List.of() : List.copyOf(components);
    }

}
