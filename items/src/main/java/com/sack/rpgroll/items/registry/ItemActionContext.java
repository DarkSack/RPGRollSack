package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemDefinition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record ItemActionContext(Player player, ItemStack itemStack, ItemDefinition definition, LivingEntity target) {
}
