package com.sack.rpgroll.guilds.guild.achievement;

import com.sack.rpgroll.guilds.guild.stats.GuildStatistics;

import java.util.List;
import java.util.function.Predicate;

/**
 * Logro/trofeo de guild — registro fijo (no autoreado en YAML, a
 * diferencia de las quests) porque son hitos de diseño del propio addon,
 * no contenido que un admin de servidor vaya a querer redefinir.
 */
public record GuildAchievementDefinition(String id, String displayName, String description,
        Predicate<GuildStatistics> condition) {

    public static final List<GuildAchievementDefinition> ALL = List.of(
            new GuildAchievementDefinition("first_boss", "Primera Sangre",
                    "Derrotá tu primer jefe como guild", stats -> stats.bossesDefeated() >= 1),
            new GuildAchievementDefinition("boss_hunter", "Cazadores de Jefes",
                    "Derrotá 25 jefes como guild", stats -> stats.bossesDefeated() >= 25),
            new GuildAchievementDefinition("first_dungeon", "Exploradores",
                    "Completá tu primera mazmorra como guild", stats -> stats.dungeonsCompleted() >= 1),
            new GuildAchievementDefinition("dungeon_master", "Maestros de Mazmorra",
                    "Completá 50 mazmorras como guild", stats -> stats.dungeonsCompleted() >= 50),
            new GuildAchievementDefinition("first_quest", "Primer Objetivo",
                    "Completá tu primera quest de guild", stats -> stats.questsCompleted() >= 1),
            new GuildAchievementDefinition("quest_veterans", "Veteranos",
                    "Completá 100 quests de guild", stats -> stats.questsCompleted() >= 100),
            new GuildAchievementDefinition("gatherers", "Recolectores",
                    "Reuní 10.000 recursos como guild", stats -> stats.resourcesGathered() >= 10_000),
            new GuildAchievementDefinition("pvp_reputation", "Temidos en el Campo",
                    "Conseguí 100 bajas PvP como guild", stats -> stats.pvpKills() >= 100));

}
