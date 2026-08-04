package com.sack.rpgroll.guilds.guild.quest;

import com.sack.rpgroll.common.content.RPGContent;

/**
 * Definición autoreada en YAML (plugins/RPGRoll-Guilds/quests/*.yml) de un
 * objetivo que las guilds pueden emprender — el progreso real (por guild)
 * vive en {@link GuildQuestProgress}, no acá.
 */
public record GuildQuestDefinition(String id, String displayName, String description, GuildQuestType type,
        String targetReference, int targetAmount, double rewardMoney, int rewardXp, int minGuildLevel)
        implements RPGContent {
}
