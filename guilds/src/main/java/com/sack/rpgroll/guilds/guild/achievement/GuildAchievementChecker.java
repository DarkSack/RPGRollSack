package com.sack.rpgroll.guilds.guild.achievement;

import com.sack.rpgroll.guilds.guild.Guild;

import java.util.ArrayList;
import java.util.List;

/** Chequea el progreso de una guild contra el registro fijo de logros y desbloquea los que correspondan. */
public class GuildAchievementChecker {

    /** @return los logros recién desbloqueados en esta pasada (vacío si ninguno). */
    public List<GuildAchievementDefinition> check(Guild guild) {

        List<GuildAchievementDefinition> unlocked = new ArrayList<>();

        for (GuildAchievementDefinition definition : GuildAchievementDefinition.ALL) {

            if (guild.unlockedAchievements().contains(definition.id())) {
                continue;
            }

            if (definition.condition().test(guild.statistics())) {
                guild.unlockAchievement(definition.id());
                unlocked.add(definition);
            }
        }

        return unlocked;
    }

}
