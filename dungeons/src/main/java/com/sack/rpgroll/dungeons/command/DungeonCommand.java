package com.sack.rpgroll.dungeons.command;

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

    public DungeonCommand(DungeonManager dungeonManager, TeamManager teamManager, DungeonEngine engine) {
        this.dungeonManager = dungeonManager;
        this.teamManager = teamManager;
        this.engine = engine;
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
        sender.sendMessage(Component.text(
                "Uso: /dungeon <list|info|invite|accept|decline|leaveparty|enter|leave|revive|ranking> [args]",
                NamedTextColor.YELLOW));
    }

    private void handleList(CommandSender sender) {

        List<DungeonDefinition> dungeons = dungeonManager.getAll().stream()
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();

        sender.sendMessage(Component.text("=== Mazmorras (" + dungeons.size() + ") ===", NamedTextColor.GOLD));

        for (DungeonDefinition definition : dungeons) {
            boolean occupied = engine.getSession(definition.id()).isPresent();
            sender.sendMessage(Component.text(" - " + definition.id() + " (", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(definition.displayName()))
                    .append(Component.text(") "))
                    .append(occupied
                            ? Component.text("[ocupada]", NamedTextColor.RED)
                            : Component.text("[libre]", NamedTextColor.GREEN)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeon info <id>", NamedTextColor.YELLOW));
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe la mazmorra '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== ", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(definition.displayName()))
                .append(Component.text(" (" + definition.id() + ") ===", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text(definition.description(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Nivel recomendado: " + definition.recommendedLevel()
                + " | Jugadores: " + definition.minPlayers() + "-" + definition.maxPlayers()
                + " | Duración estimada: " + definition.estimatedMinutes() + " min", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Salas: " + definition.rooms().size() + " | Repetible: "
                + (definition.repeatable() ? "sí" : "no"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Dificultades: "
                + String.join(", ", definition.difficulties().stream().map(d -> d.displayName()).toList()),
                NamedTextColor.GRAY));
    }

    private void handleInvite(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede invitar.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeon invite <jugador>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return;
        }

        var result = teamManager.invite(player, target);

        if (result == TeamManager.InviteResult.OK) {
            player.sendMessage(Component.text("✔ Invitación enviada a " + target.getName() + ".",
                    NamedTextColor.GREEN));
            target.sendMessage(Component.text(player.getName()
                    + " te invitó a su grupo de mazmorra — /dungeon accept (60s)", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("No se pudo invitar: " + result, NamedTextColor.RED));
        }
    }

    private void handleAccept(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            return;
        }

        var result = teamManager.accept(player);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Te uniste al grupo.", NamedTextColor.GREEN));
            case NO_INVITE -> player.sendMessage(Component.text("No tenés ninguna invitación pendiente.",
                    NamedTextColor.RED));
            case EXPIRED -> player.sendMessage(Component.text("Esa invitación ya expiró.", NamedTextColor.RED));
            case TEAM_GONE -> player.sendMessage(Component.text("Ese grupo ya no existe.", NamedTextColor.RED));
            case TEAM_FULL -> player.sendMessage(Component.text("Ese grupo ya está lleno.", NamedTextColor.RED));
        }
    }

    private void handleDecline(CommandSender sender) {

        if (sender instanceof Player player) {
            teamManager.decline(player.getUniqueId());
            player.sendMessage(Component.text("Invitación rechazada.", NamedTextColor.GRAY));
        }
    }

    private void handleLeaveParty(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            return;
        }

        boolean disbanded = teamManager.leave(player.getUniqueId());
        player.sendMessage(Component.text(disbanded ? "Grupo disuelto." : "Saliste del grupo.",
                NamedTextColor.GRAY));
    }

    private void handleEnter(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede entrar a una mazmorra.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeon enter <id> [dificultad]", NamedTextColor.YELLOW));
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            player.sendMessage(Component.text("No existe la mazmorra '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        String difficultyId = args.length >= 3 ? args[2] : definition.difficulties().get(0).id();

        Team team = teamManager.getTeam(player.getUniqueId())
                .orElseGet(() -> soloTeam(player));

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(Component.text("Solo el líder del grupo puede iniciar la mazmorra.",
                    NamedTextColor.RED));
            return;
        }

        var result = engine.tryEnter(team, definition, difficultyId);

        switch (result) {
            case OK_STARTED -> {
            }
            case OK_QUEUED -> team.members().forEach(memberId -> {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(Component.text("La mazmorra está ocupada — tu grupo quedó en cola.",
                            NamedTextColor.YELLOW));
                }
            });
            case ALREADY_RUNNING -> player.sendMessage(Component.text("Tu grupo ya está en una corrida.",
                    NamedTextColor.RED));
            case PARTY_TOO_SMALL -> player.sendMessage(Component.text(
                    "Se necesitan al menos " + definition.minPlayers() + " jugador(es).", NamedTextColor.RED));
            case PARTY_TOO_LARGE -> player.sendMessage(Component.text(
                    "El grupo excede el máximo de " + definition.maxPlayers() + " jugador(es).", NamedTextColor.RED));
            case ON_COOLDOWN -> player.sendMessage(Component.text(
                    "Alguien del grupo todavía está en cooldown para esta mazmorra.", NamedTextColor.RED));
            case DIFFICULTY_NOT_FOUND -> player.sendMessage(Component.text(
                    "No existe la dificultad '" + difficultyId + "'.", NamedTextColor.RED));
        }
    }

    /** No está en un grupo formado — se le arma uno de un solo integrante, sin registrarlo en el manager. */
    private Team soloTeam(Player player) {
        return new Team(player.getUniqueId());
    }

    private void handleLeave(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            return;
        }

        var sessionOpt = engine.findSessionByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            player.sendMessage(Component.text("No estás en ninguna corrida.", NamedTextColor.RED));
            return;
        }

        var session = sessionOpt.get();
        DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);

        if (definition != null) {
            engine.abandon(session, definition);
            player.sendMessage(Component.text("Abandonaste la mazmorra.", NamedTextColor.GRAY));
        }
    }

    private void handleRevive(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeon revive <jugador>", NamedTextColor.YELLOW));
            return;
        }

        var sessionOpt = engine.findSessionByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            player.sendMessage(Component.text("No estás en ninguna corrida.", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return;
        }

        var session = sessionOpt.get();
        DungeonDefinition definition = dungeonManager.get(session.dungeonId()).orElse(null);

        if (definition == null) {
            return;
        }

        boolean revived = engine.revive(session, definition, target.getUniqueId());
        player.sendMessage(Component.text(revived ? "✔ Revivido." : "Ese jugador no está muerto en esta corrida.",
                revived ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void handleRanking(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeon ranking <id> [daily|weekly|monthly|global]",
                    NamedTextColor.YELLOW));
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe la mazmorra '" + args[1] + "'.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("Todavía no hay corridas registradas.", NamedTextColor.GRAY));
            return;
        }

        for (int i = 0; i < top.size(); i++) {
            DungeonRunResult run = top.get(i);
            sender.sendMessage(Component.text((i + 1) + ". " + String.join(", ", run.playerNames())
                    + " — " + (run.durationMillis() / 1000) + "s, " + run.deaths() + " muerte(s)",
                    NamedTextColor.GRAY));
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
