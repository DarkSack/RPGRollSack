package com.sack.rpgroll.gameplay.enchant.effect.handlers;

import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectHandler;
import org.bukkit.entity.LivingEntity;

/**
 * Prende fuego al objetivo. Duración = duration-ticks-per-level * nivel.
 */
public class SetFireEffectHandler implements EnchantEffectHandler {

    @Override
    public void execute(EnchantEffectContext context) {

        LivingEntity target = context.getTarget();
        if (target == null) {
            return;
        }

        int durationPerLevel = context.getEnchantment().getParamInt("duration-ticks-per-level", 40);
        int ticks = durationPerLevel * context.getLevel();

        target.setFireTicks(Math.max(target.getFireTicks(), ticks));
    }

}