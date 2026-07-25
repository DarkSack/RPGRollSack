package com.sack.rpgroll.gameplay.enchant.effect;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Datos disponibles para un handler de efecto al ejecutarse. No todos los
 * campos aplican a todos los triggers — ej. target/damageEvent solo tienen
 * valor en ON_HIT, quedan null en PASSIVE.
 */
public class EnchantEffectContext {

    private final Player player;
    private final ItemStack item;
    private final int level;
    private final CustomEnchantment enchantment;

    private LivingEntity target;
    private EntityDamageByEntityEvent damageEvent;

    public EnchantEffectContext(Player player, ItemStack item, int level, CustomEnchantment enchantment) {
        this.player = player;
        this.item = item;
        this.level = level;
        this.enchantment = enchantment;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getLevel() {
        return level;
    }

    public CustomEnchantment getEnchantment() {
        return enchantment;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public EntityDamageByEntityEvent getDamageEvent() {
        return damageEvent;
    }

    public void setDamageEvent(EntityDamageByEntityEvent damageEvent) {
        this.damageEvent = damageEvent;
    }

}