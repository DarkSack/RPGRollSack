package com.sack.rpgroll.quests.condition;

import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.entity.Player;

public record QuestConditionContext(Player player, RPGPlayer rpgPlayer) {
}
