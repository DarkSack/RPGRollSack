package com.sack.rpgroll.gameplay.enchant.effect.handlers;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Aplica un efecto de poción al portador mientras tenga la pieza de
 * armadura equipada (ej. Salto de Liebre = Jump Boost). Reaplicado
 * periódicamente por PassiveEnchantTask — la duración cubre el intervalo
 * entre ejecuciones con margen, para que no "parpadee" el efecto.
 */
public class PotionEffectSelfPassiveHandler implements EnchantEffectHandler {

    private static final int REFRESH_DURATION_TICKS = 80; // margen sobre el período de la tarea (40 ticks)

    @Override
    public void execute(EnchantEffectContext context) {

        Player player = context.getPlayer();
        CustomEnchantment enchant = context.getEnchantment();
        String potionName = enchant.getParamString("potion-effect", null);

        if (potionName == null) {
            return;
        }

        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(potionName.toLowerCase()));
        if (type == null) {
            return;
        }

        int amplifierPerLevel = enchant.getParamInt("amplifier-per-level", 1);
        int amplifier = (context.getLevel() - 1) * amplifierPerLevel;

        player.addPotionEffect(new PotionEffect(type, REFRESH_DURATION_TICKS, amplifier, false, false, false));
    }

}