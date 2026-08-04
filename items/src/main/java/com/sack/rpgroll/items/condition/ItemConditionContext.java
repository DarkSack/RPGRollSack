package com.sack.rpgroll.items.condition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public record ItemConditionContext(Player player, LivingEntity target) {
}
