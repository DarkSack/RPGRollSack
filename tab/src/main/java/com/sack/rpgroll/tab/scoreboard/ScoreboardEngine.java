package com.sack.rpgroll.tab.scoreboard;

import com.sack.rpgroll.tab.animation.AnimationDefinition;
import com.sack.rpgroll.tab.animation.AnimationEngine;
import com.sack.rpgroll.tab.animation.AnimationManager;
import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.tab.teams.PlayerScoreboardService;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renderiza el sidebar en el tablero INDIVIDUAL del propio jugador (a
 * diferencia de Teams/BelowName, el sidebar es sobre lo que VOS ves, no
 * sobre cómo te ven a vos). Cada línea es una entrada invisible
 * (secuencia {@code §<hex>}) con un Team que le pone el texto real como
 * prefix/suffix — es la técnica estándar para líneas de sidebar con texto
 * arbitrario, ya que vanilla no expone una API de "línea = Component libre".
 */
public class ScoreboardEngine {

    private static final String OBJECTIVE_NAME = "rpgtab_sidebar";
    private static final String HEX = "0123456789abcdef";
    private static final int MAX_LINE_LENGTH = 64;

    private final PlayerScoreboardService scoreboardService;
    private final PlaceholderEngine placeholderEngine;
    private final LineConditionEvaluator conditionEvaluator;
    private final AnimationManager animationManager;
    private final AnimationEngine animationEngine;

    public ScoreboardEngine(PlayerScoreboardService scoreboardService, PlaceholderEngine placeholderEngine,
            LineConditionEvaluator conditionEvaluator, AnimationManager animationManager,
            AnimationEngine animationEngine) {
        this.scoreboardService = scoreboardService;
        this.placeholderEngine = placeholderEngine;
        this.conditionEvaluator = conditionEvaluator;
        this.animationManager = animationManager;
        this.animationEngine = animationEngine;
    }

    public void apply(Player player, ScoreboardDefinition definition) {

        Scoreboard board = scoreboardService.getOrCreate(player);
        Component title = renderTitle(definition, player);

        Objective objective = board.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.displayName(title);
        }

        List<String> visibleLines = definition.lines().stream()
                .filter(line -> conditionEvaluator.evaluate(line.condition(), player))
                .map(ScoreboardLine::text)
                .toList();

        syncEntries(board, objective, visibleLines, player);
    }

    public void clear(Player player) {

        Scoreboard board = scoreboardService.getOrCreate(player);
        Objective objective = board.getObjective(OBJECTIVE_NAME);

        if (objective != null) {
            syncEntries(board, objective, List.of(), player);
            objective.unregister();
        }
    }

    private Component renderTitle(ScoreboardDefinition definition, Player player) {

        String rawTitle = definition.title();

        if (definition.titleAnimationId() != null) {

            AnimationDefinition animation = animationManager.get(definition.titleAnimationId()).orElse(null);

            if (animation != null) {
                String frame = animationEngine.currentFrame(animation);
                if (frame != null) {
                    rawTitle = frame;
                }
            }
        }

        return ComponentUtils.parse(placeholderEngine.resolve(rawTitle, player));
    }

    private void syncEntries(Scoreboard board, Objective objective, List<String> lines, Player player) {

        Set<String> desiredEntries = new HashSet<>();
        int score = lines.size();

        for (int i = 0; i < lines.size(); i++) {

            String entry = entryFor(i);
            desiredEntries.add(entry);

            Team team = board.getTeam(entry);
            if (team == null) {
                team = board.registerNewTeam(entry);
            }

            String rendered = placeholderEngine.resolve(lines.get(i), player);
            applyLineText(team, rendered);

            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }

            objective.getScore(entry).setScore(score--);
        }

        for (String entry : new ArrayList<>(board.getEntries())) {

            if (isSidebarEntry(entry) && !desiredEntries.contains(entry)) {

                board.resetScores(entry);
                Team team = board.getTeam(entry);

                if (team != null) {
                    team.unregister();
                }
            }
        }
    }

    private void applyLineText(Team team, String rendered) {

        if (rendered.length() <= MAX_LINE_LENGTH) {
            team.prefix(Component.empty());
            team.suffix(ComponentUtils.parse(rendered));
            return;
        }

        team.prefix(ComponentUtils.parse(rendered.substring(0, MAX_LINE_LENGTH)));
        team.suffix(ComponentUtils.parse(rendered.substring(MAX_LINE_LENGTH,
                Math.min(rendered.length(), MAX_LINE_LENGTH * 2))));
    }

    private String entryFor(int index) {

        if (index < HEX.length()) {
            return "§" + HEX.charAt(index);
        }

        return "§" + HEX.charAt(index / HEX.length()) + "§" + HEX.charAt(index % HEX.length());
    }

    private boolean isSidebarEntry(String entry) {
        return entry.length() <= 4 && entry.startsWith("§");
    }

}
