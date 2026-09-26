package com.sack.rpgroll.quests.condition;

import com.sack.rpgroll.common.character.RPGCharacters;

import org.bukkit.entity.Player;

public record QuestConditionContext(Player player, RPGCharacters characters) {
}
