package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Un hechizo completo — identidad, escuela, costo, cast, cooldown, su lugar
 * en el árbol de progresión de su escuela, y el pipeline de {@link
 * SpellComponent} que define qué hace. El árbol de progresión no es una
 * entidad separada: {@code treeParentId} apunta al id de otro hechizo de la
 * misma escuela que hace falta haber aprendido antes.
 *
 * @param level        nivel mínimo de jugador para aprenderlo (vía Grimoire)
 * @param castTimeTicks ticks de canalización si el trigger es HOLD (ignorado en LEFT_CLICK/RIGHT_CLICK)
 * @param cooldownTicks cooldown por jugador tras lanzarlo
 * @param treeParentId id de otro hechizo de la misma escuela requerido antes de poder aprender este, o null
 * @param treeTier     nivel dentro del árbol, puramente para ordenar la visualización
 */
public record Spell(
        String id,
        String displayName,
        String icon,
        String color,
        String schoolId,
        SpellRarity rarity,
        int level,
        SpellCost cost,
        int castTimeTicks,
        int cooldownTicks,
        SpellCastTrigger trigger,
        String treeParentId,
        int treeTier,
        Set<String> tags,
        String description,
        List<SpellComponent> components) implements RPGContent {

    public Spell {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "BLAZE_POWDER" : icon;
        color = color == null || color.isBlank() ? "WHITE" : color;
        Objects.requireNonNull(schoolId, "schoolId no puede ser null");
        rarity = rarity == null ? SpellRarity.COMMON : rarity;
        level = Math.max(0, level);
        cost = cost == null ? SpellCost.none() : cost;
        castTimeTicks = Math.max(0, castTimeTicks);
        cooldownTicks = Math.max(0, cooldownTicks);
        trigger = trigger == null ? SpellCastTrigger.RIGHT_CLICK : trigger;
        treeParentId = (treeParentId == null || treeParentId.isBlank()) ? null : treeParentId;
        treeTier = Math.max(0, treeTier);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        description = description == null ? "" : description;
        components = components == null ? List.of() : List.copyOf(components);
    }

    public boolean hasTreeParent() {
        return treeParentId != null;
    }

}
