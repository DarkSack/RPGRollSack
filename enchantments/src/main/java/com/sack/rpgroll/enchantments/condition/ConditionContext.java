package com.sack.rpgroll.enchantments.condition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Datos disponibles para evaluar las condiciones de un encantamiento en el
 * momento en que su trigger se disparó. {@code target} es nulo cuando el
 * trigger no tiene un objetivo natural (ej. BLOCK_BREAK).
 */
public record ConditionContext(Player player, LivingEntity target) {
}
