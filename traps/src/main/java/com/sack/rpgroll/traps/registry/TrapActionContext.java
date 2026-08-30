package com.sack.rpgroll.traps.registry;

import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.engine.TrapEngine;
import com.sack.rpgroll.traps.location.PlacedTrap;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Contexto disponible al ejecutar una {@link com.sack.rpgroll.traps.core.TrapAction}.
 * Compartido entre trampas Y torretas — por eso {@code placed}/{@code definition}/
 * {@code engine} son nullable (una torreta no tiene ninguno de los tres) y el
 * objetivo es un {@link LivingEntity} genérico (jugador o mob hostil), no solo
 * {@link Player}: una torreta también puede dañar/aplicar efectos a un mob.
 * {@code engine} solo lo usa TRIGGER_TRAP (para forzar el disparo de otra
 * trampa de la cadena) — queda sin efecto (con warning en log) si viene null,
 * ej. cuando la acción se ejecuta desde una torreta.
 */
public record TrapActionContext(PlacedTrap placed, TrapDefinition definition, LivingEntity target, TrapEngine engine,
        Location anchorOverride, String sourceLabel) {

    public Location anchorLocation() {
        return placed != null ? placed.primaryLocation() : anchorOverride;
    }

    /** Deriva el Player solo si el objetivo lo es — acciones que no tienen sentido contra un mob (MESSAGE, TELEPORT, FIRE, COMMAND_AS_PLAYER) siguen usando esto y no-opean solas contra un mob. */
    public Player triggeringPlayer() {
        return target instanceof Player p ? p : null;
    }

    /** Identificador legible para logs — el id de la TrapDefinition, o {@code sourceLabel} cuando no hay una (torreta). */
    public String sourceId() {
        return definition != null ? definition.id() : sourceLabel;
    }

}
