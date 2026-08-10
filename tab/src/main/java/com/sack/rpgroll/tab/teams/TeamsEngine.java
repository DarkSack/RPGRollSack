package com.sack.rpgroll.tab.teams;

import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Registra el Team de {@code subject} en el tablero INDIVIDUAL de cada
 * {@code viewer} (ver {@link PlayerScoreboardService}) — así cada viewer
 * puede tener su propio sidebar sin perder los colores de nametag de los
 * demás, y en el futuro un mismo jugador puede verse distinto según quién
 * lo mire (sección 11) simplemente registrando un Team distinto en el
 * tablero de un viewer específico.
 */
public class TeamsEngine {

    private static final String TEAM_PREFIX = "rpgtab_";

    private final PlayerScoreboardService scoreboardService;

    public TeamsEngine(PlayerScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }

    public void apply(Player subject, TeamsDefinition definition, int sortRank, Collection<Player> viewers) {

        String teamName = buildTeamName(sortRank, subject.getUniqueId());

        for (Player viewer : viewers) {
            applyOnBoard(subject, definition, teamName, scoreboardService.getOrCreate(viewer));
        }
    }

    /** Igual que {@link #apply}, pero solo sobre el tablero de UN viewer — usado para overrides por-observador. */
    public void applyForViewer(Player subject, TeamsDefinition definition, int sortRank, Player viewer) {
        applyOnBoard(subject, definition, buildTeamName(sortRank, subject.getUniqueId()),
                scoreboardService.getOrCreate(viewer));
    }

    public void remove(Player subject, Collection<Player> viewers) {

        for (Player viewer : viewers) {

            Scoreboard board = scoreboardService.getOrCreate(viewer);
            Team existing = board.getEntryTeam(subject.getName());

            if (existing != null && existing.getName().startsWith(TEAM_PREFIX)) {
                existing.removeEntry(subject.getName());
                if (existing.getEntries().isEmpty()) {
                    existing.unregister();
                }
            }
        }
    }

    private void applyOnBoard(Player subject, TeamsDefinition definition, String teamName, Scoreboard board) {

        migrateIfNeeded(subject, board, teamName);

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.prefix(ComponentUtils.parse(definition.prefix()));
        team.suffix(ComponentUtils.parse(definition.suffix()));

        NamedTextColor color = resolveColor(definition.color());
        if (color != null) {
            team.color(color);
        }

        team.setAllowFriendlyFire(definition.allowFriendlyFire());
        team.setCanSeeFriendlyInvisibles(definition.canSeeFriendlyInvisibles());
        team.setOption(Team.Option.COLLISION_RULE, parseOptionStatus(definition.collisionRule()));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, parseOptionStatus(definition.nametagVisibility()));

        if (!team.hasEntry(subject.getName())) {
            team.addEntry(subject.getName());
        }
    }

    private void migrateIfNeeded(Player subject, Scoreboard board, String newTeamName) {

        Team existing = board.getEntryTeam(subject.getName());

        if (existing == null || existing.getName().equals(newTeamName) || !existing.getName().startsWith(TEAM_PREFIX)) {
            return;
        }

        existing.removeEntry(subject.getName());

        if (existing.getEntries().isEmpty()) {
            existing.unregister();
        }
    }

    private String buildTeamName(int sortRank, UUID uuid) {

        String rankPart = String.format(Locale.ROOT, "%05d", Math.max(0, Math.min(99999, sortRank)));
        String uuidPart = uuid.toString().replace("-", "").substring(0, 8);

        return TEAM_PREFIX + rankPart + "_" + uuidPart;
    }

    private NamedTextColor resolveColor(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        return NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
    }

    private Team.OptionStatus parseOptionStatus(String raw) {

        try {
            return Team.OptionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Team.OptionStatus.ALWAYS;
        }
    }

}
