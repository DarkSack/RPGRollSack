package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.progress.CriterionParser;
import com.sack.rpgroll.ascension.reward.RewardsParser;
import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

public class FactionParser implements ContentParser<Faction> {

    @Override
    public Faction parse(YamlConfiguration config) {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Faction(id,
                config.getString("display-name", id),
                parseRanks(config.getConfigurationSection("ranks")),
                CriterionParser.parseList(id, config.getMapList("sources"), true),
                config.getStringList("rivals"),
                config.getDouble("rival-penalty", 0.5),
                config.getInt("min", -10000),
                config.getInt("max", 10000));
    }

    private List<FactionRank> parseRanks(ConfigurationSection section) {

        List<FactionRank> ranks = new ArrayList<>();

        if (section == null) {
            return ranks;
        }

        for (String rankId : section.getKeys(false)) {

            ConfigurationSection rank = section.getConfigurationSection(rankId);
            if (rank == null) {
                continue;
            }

            ranks.add(new FactionRank(rankId, rank.getString("display-name", rankId), rank.getInt("threshold", 0),
                    RewardsParser.parse(rank.getConfigurationSection("rewards"))));
        }

        return ranks;
    }

}
