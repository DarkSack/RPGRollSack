package com.sack.rpgroll.gameplay.enchant.effect.handlers;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Aplica un efecto de poción al objetivo golpeado (ej. Lentitud para
 * "Congelamiento"). El nombre en params.potion-effect debe coincidir con
 * una clave vanilla (ej. "SLOWNESS", "WEAKNESS").
 */
public class PotionEffectTargetHandler implements EnchantEffectHandler {

    @Override
    public void execute(EnchantEffectContext context) {

        LivingEntity target = context.getTarget();
        if (target == null) {
            return;
        }

        CustomEnchantment enchant = context.getEnchantment();
        String potionName = enchant.getParamString("potion-effect", null);
        if (potionName == null) {
            return;
        }

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(potionName.toLowerCase()));
        if (type == null) {
            return;
        }

        int durationTicks = enchant.getParamInt("duration-ticks", 60);
        int amplifierPerLevel = enchant.getParamInt("amplifier-per-level", 1);
        int amplifier = (context.getLevel() - 1) * amplifierPerLevel;

        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, true, true));
    }

}