package com.sack.rpgroll.enchantments.effect;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Datos disponibles para ejecutar los efectos de un encantamiento:
 * quién lo activó, contra quién (si aplica) y los datos numéricos del
 * nivel actual (para resolver placeholders "{clave}" en los params).
 */
public record EffectContext(Player player, LivingEntity target, int level, Map<String, Double> levelData) {
}
