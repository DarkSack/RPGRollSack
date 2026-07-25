package com.sack.rpgroll.gameplay.enchant.effect.handlers;

import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectHandler;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Cura al atacante un porcentaje del daño infligido (basado en el daño final
 * del evento). percent = percent-per-level * nivel.
 */
public class LifestealEffectHandler implements EnchantEffectHandler {

    @Override
    public void execute(EnchantEffectContext context) {

        EntityDamageByEntityEvent event = context.getDamageEvent();
        if (event == null) {
            return;
        }

        double percentPerLevel = context.getEnchantment().getParamDouble("percent-per-level", 5.0);
        double percent = percentPerLevel * context.getLevel();
        double healAmount = event.getFinalDamage() * (percent / 100.0);

        if (healAmount <= 0) {
            return;
        }

        Player player = context.getPlayer();
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;

        player.setHealth(Math.min(player.getHealth() + healAmount, maxHealth));
    }

}