package com.sack.rpgroll.guilds.guild.ranking;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Rankings de guild (spec: "nivel/riqueza/PvP/jefes/dungeons/reputación/
 * miembros"). A diferencia del ranking de corridas de Dungeons, esto se
 * deriva en vivo de los datos actuales de cada guild — no hace falta un
 * historial separado.
 */
public class GuildRankingManager {

    public enum Category {
        LEVEL(guild -> guild.level()),
        WEALTH(guild -> guild.vault().balance()),
        PVP(guild -> guild.statistics().pvpKills()),
        BOSSES(guild -> guild.statistics().bossesDefeated()),
        DUNGEONS(guild -> guild.statistics().dungeonsCompleted()),
        REPUTATION(guild -> guild.reputation().values().stream().mapToInt(Integer::intValue).sum()),
        MEMBERS(guild -> guild.memberCount());

        private final ToDoubleFunction<Guild> extractor;

        Category(ToDoubleFunction<Guild> extractor) {
            this.extractor = extractor;
        }

        public double valueOf(Guild guild) {
            return extractor.applyAsDouble(guild);
        }
    }

    private final GuildManager guildManager;

    public GuildRankingManager(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    public List<Guild> top(Category category, int limit) {
        return guildManager.getAll().stream()
                .sorted(Comparator.comparingDouble((Guild guild) -> category.valueOf(guild)).reversed())
                .limit(limit)
                .toList();
    }

}
