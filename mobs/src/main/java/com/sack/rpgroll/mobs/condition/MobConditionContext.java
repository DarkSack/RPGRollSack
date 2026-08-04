package com.sack.rpgroll.mobs.condition;

import org.bukkit.entity.LivingEntity;

public record MobConditionContext(LivingEntity mob, LivingEntity target) {
}
