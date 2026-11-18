package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemDefinition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

public record ItemActionContext(Player player, ItemStack itemStack, ItemDefinition definition, LivingEntity target,
                                EquipmentSlot hand) {
    public ItemActionContext(Player player, ItemStack itemStack, ItemDefinition definition, LivingEntity target) {
        this(player, itemStack, definition, target, EquipmentSlot.HAND);
    }
}
