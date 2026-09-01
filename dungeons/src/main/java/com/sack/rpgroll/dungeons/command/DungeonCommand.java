package com.sack.rpgroll.dungeons.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.ranking.DungeonRunResult;
import com.sack.rpgroll.dungeons.ranking.RankingPeriod;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /dungeon list|info|invite|accept|decline|leaveparty|enter|leave|revive|ranking
 */
public class DungeonCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "info", "invite", "accept", "decline",
            "leaveparty", "enter", "leave", "revive", "ranking");

    private final DungeonManager dungeonManager;
    private final TeamManager teamManager;
    private final DungeonEngine engine;
    private final LangManager lang;

    public DungeonCommand(DungeonManager dungeonManager, TeamManager teamManager, DungeonEngine engine,
            LangManager lang) {
        this.dungeonManager = dungeonManager;
        this.teamManager = teamManager;
        this.engine = engine;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "invite" -> handleInvite(sender, args);
            case "accept" -> handleAccept(sender);
            case "decline" -> handleDecline(sender);
            case "leaveparty" -> handleLeaveParty(sender);
            case "enter" -> handleEnter(sender, args);
            case "leave" -> handleLeave(sender);
            case "revive" -> handleRevive(sender, args);
            case "ranking" -> handleRanking(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.dungeon.usage");
    }

    private void handleList(CommandSender sender) {

        List<DungeonDefinition> dungeons = dungeonManager.getAll().stream()
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();

        lang.send(sender, "command.dungeon.list.header", "count", dungeons.size());

        for (DungeonDefinition definition : dungeons) {
            boolean occupied = engine.getSession(definition.id()).isPresent();
            sender.sendMessage(Component.text(" - " + definition.id() + " (", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(definition.displayName()))
                    .append(Component.text(") "))
                    .append(occupied
                            ? ComponentUtils.parse(lang.raw("command.dungeon.list.occupied"))
                            : ComponentUtils.parse(lang.raw("command.dungeon.list.free"))));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.dungeon.info.usage");
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.dungeon.not_found", "id", args[1]);
            return;
        }

        sender.sendMessage(Component.text("=== ", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(definition.displayName()))
                .append(Component.text(" (" + definition.id() + ") ===", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text(definition.description(), NamedTextColor.GRAY));
        lang.send(sender, "command.dungeon.info.stats", "level", definition.recommendedLevel(),
                "min", definition.minPlayers(), "max", definition.maxPlayers(),
                "minutes", definition.estimatedMinutes());
        lang.send(sender, "command.dungeon.info.rooms", "rooms", definition.rooms().size(),
                "repeatable", lang.raw(definition.repeatable() ? "common.yes" : "common.no"));
        lang.send(sender, "command.dungeon.info.difficulties", "difficulties",
                String.join(", ", definition.difficulties().stream().map(d -> d.displayName()).toList()));
    }

    private void handleInvite(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeon.invite.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.dungeon.invite.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "command.dungeon.player_not_found");
            return;
        }

        var result = teamManager.invite(player, target);

        if (result == TeamManager.InviteResult.OK) {
            lang.send(player, "command.dungeon.invite.sent", "target", target.getName());
            lang.send(target, "command.dungeon.invite.received", "inviter", player.getName());
        } else {
            lang.send(player, "command.dungeon.invite.failed", "result", result);
        }
    }

    private void handleAccept(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            return;
        }

        var result = teamManager.accept(player);

        switch (result) {
            case OK -> lang.send(player, "command.dungeon.accept.ok");
            case NO_INVITE -> lang.send(player, "command.dungeon.accept.no_invite");
            case EXPIRED -> lang.send(player, "command.dungeon.accept.expired");
            case TEAM_GONE -> lang.send(player, "command.dungeon.accept.team_gone");
            case TEAM_FULL -> lang.send(player, "command.dungeon.accept.team_full");
        }
    }

    private void handleDecline(CommandSender sender) {

        if (Senders.asPlayer(sender) instanceof Player player) {
            teamManager.decline(player.getUniqueId());
            lang.send(player, "command.dungeon.decline.ok");
        }
    }

    private void handleLeaveParty(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            // Sin este aviso el comando no respondía absolutamente nada desde
            // consola, y no había forma de saber si había hecho algo.
            lang.send(sender, "command.dungeon.leaveparty.players_only");
            return;
        }

        boolean disbanded = teamManager.leave(player.getUniqueId());
        lang.send(player, disbanded ? "command.dungeon.leaveparty.disbanded" : "command.dungeon.leaveparty.left");
    }

    private void handleEnter(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeon.enter.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.dungeon.enter.usage");
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(player, "command.dungeon.not_found", "id", args[1]);
            return;
        }

        String difficultyId = args.length >= 3 ? args[2] : definition.difficulties().get(0).id();

        Team team = teamManager.getTeam(player.getUniqueId())
                .orElseGet(() -> soloTeam(player));

        if (!team.isLeader(player.getUniqueId())) {
            lang.send(player, "command.dungeon.enter.not_leader");
            return;
        }

        var result = engine.tryEnter(team, definition, difficultyId);

        switch (result) {
            case OK_STARTED -> {
            }
            case OK_QUEUED -> team.members().forEach(memberId -> {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    lang.send(member, "command.dungeon.enter.queued");
                }
            });
            case ALREADY_RUNNING -> lang.send(player, "command.dungeon.enter.already_running");
            case PARTY_TOO_SMALL -> lang.send(player, "command.dungeon.enter.party_too_small",
                    "min", definition.minPlayers());
            case PARTY_TOO_LARGE -> lang.send(player, "command.dungeon.enter.party_too_large",
                    "max", definition.maxPlayers());
            case ON_COOLDOWN -> lang.send(player, "command.dungeon.enter.on_cooldown");
            case DIFFICULTY_NOT_FOUND -> lang.send(player, "command.dungeon.enter.difficulty_not_found",
                    "difficulty", difficultyId);
        }
    }

    /** No está en un grupo formado — se le arma uno de un solo integrante, sin registrarlo en el manager. */
    private Team soloTeam(Player player) {
        return new Team(player.getUniqueId());
    }

    private void handleLeave(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeon.leave.players_only");
            return;
        }

        var sessionOpt = engine.findSessionByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            lang.send(player, "command.dungeon.not_in_run");
            return;
        }

        var session = sessionOpt.get();
        DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);

        if (definition != null) {
            engine.abandon(session, definition);
            lang.send(player, "command.dungeon.leave.ok");
        }
    }

    private void handleRevive(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeon.revive.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.dungeon.revive.usage");
            return;
        }

        var sessionOpt = engine.findSessionByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            lang.send(player, "command.dungeon.not_in_run");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(player, "command.dungeon.player_not_found");
            return;
        }

        var session = sessionOpt.get();
        DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);

        if (definition == null) {
            return;
        }

        boolean revived = engine.revive(session, definition, target.getUniqueId());
        lang.send(player, revived ? "command.dungeon.revive.ok" : "command.dungeon.revive.not_dead");
    }

    private void handleRanking(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.dungeon.ranking.usage");
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.dungeon.not_found", "id", args[1]);
            return;
        }

        RankingPeriod period = RankingPeriod.GLOBAL;
        if (args.length >= 3) {
            try {
                period = RankingPeriod.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<DungeonRunResult> top = engine.getRankingManager().top(definition.id(), period, 10);

        sender.sendMessage(Component.text("=== Ranking ", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(definition.displayName()))
                .append(Component.text(" (" + period + ") ===", NamedTextColor.GOLD)));

        if (top.isEmpty()) {
            lang.send(sender, "command.dungeon.ranking.empty");
            return;
        }

        for (int i = 0; i < top.size(); i++) {
            DungeonRunResult run = top.get(i);
            lang.send(sender, "command.dungeon.ranking.entry", "position", (i + 1),
                    "players", String.join(", ", run.playerNames()),
                    "seconds", (run.durationMillis() / 1000), "deaths", run.deaths());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            return switch (sub) {
                case "info", "enter", "ranking" -> TabCompleteUtil.filter(args[1], dungeonIds());
                case "invite", "revive" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "enter" -> {
                    DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
                    yield definition == null ? List.of()
                            : TabCompleteUtil.filter(args[2],
                                    definition.difficulties().stream().map(d -> d.id()).toList());
                }
                case "ranking" -> TabCompleteUtil.filter(args[2], java.util.Arrays.stream(RankingPeriod.values())
                        .map(Enum::name).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> dungeonIds() {
        return dungeonManager.getAll().stream().map(DungeonDefinition::id).toList();
    }

}
