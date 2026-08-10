package com.sack.rpgroll.tab.belowname;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.tab.teams.PlayerScoreboardService;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;

/** Igual que {@link com.sack.rpgroll.tab.teams.TeamsEngine}: escribe en el tablero individual de cada viewer. */
public class BelowNameEngine {

    private static final String OBJECTIVE_NAME = "rpgtab_belowname";

    private final PlayerScoreboardService scoreboardService;
    private final PlaceholderEngine placeholderEngine;

    public BelowNameEngine(PlayerScoreboardService scoreboardService, PlaceholderEngine placeholderEngine) {
        this.scoreboardService = scoreboardService;
        this.placeholderEngine = placeholderEngine;
    }

    public void apply(Player subject, BelowNameDefinition definition, Collection<Player> viewers) {

        int score = parseScore(placeholderEngine.resolve(definition.scorePlaceholder(), subject));

        for (Player viewer : viewers) {

            Scoreboard board = scoreboardService.getOrCreate(viewer);
            Objective objective = board.getObjective(OBJECTIVE_NAME);

            if (objective == null) {
                objective = board.registerNewObjective(
                        OBJECTIVE_NAME, Criteria.DUMMY, ComponentUtils.parse(definition.label()));
                objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            } else {
                objective.displayName(ComponentUtils.parse(definition.label()));
            }

            objective.getScore(subject.getName()).setScore(score);
        }
    }

    public void remove(Player subject, Collection<Player> viewers) {

        for (Player viewer : viewers) {

            Scoreboard board = scoreboardService.getOrCreate(viewer);

            if (board.getObjective(OBJECTIVE_NAME) != null) {
                board.resetScores(subject.getName());
            }
        }
    }

    private int parseScore(String value) {

        if (value == null) {
            return 0;
        }

        try {
            return (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
