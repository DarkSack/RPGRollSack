package com.sack.rpgroll.traps.condition;

import org.bukkit.entity.Player;

/** Contexto disponible al evaluar una condición de trampa — puede no haber jugador (ej. disparo por TIMER). */
public record TrapConditionContext(Player player) {
}
