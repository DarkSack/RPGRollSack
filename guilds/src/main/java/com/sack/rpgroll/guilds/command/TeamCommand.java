package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.guilds.gui.team.TeamHubGUI;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.chat.TeamChatListener;
import com.sack.rpgroll.guilds.team.matchmaking.MatchmakingRequest;
import com.sack.rpgroll.guilds.team.matchmaking.TeamMatchmakingQueue;
import com.sack.rpgroll.guilds.team.ping.PingType;
import com.sack.rpgroll.guilds.team.ping.TeamPingManager;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Locale;

/** /team create|invite|accept|decline|leave|kick|promote|info|config|buff|ping|waypoint|chat|queue */
public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final TeamMatchmakingQueue matchmakingQueue;
    private final TeamPingManager pingManager;
    private final TeamChatListener chatListener;
    private final ChatPromptManager chatPromptManager;

    public TeamCommand(TeamManager teamManager, TeamMatchmakingQueue matchmakingQueue, TeamPingManager pingManager,
            TeamChatListener chatListener, ChatPromptManager chatPromptManager) {
        this.teamManager = teamManager;
        this.matchmakingQueue = matchmakingQueue;
        this.pingManager = pingManager;
        this.chatListener = chatListener;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            new TeamHubGUI(player, teamManager, chatPromptManager).open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "decline" -> {
                teamManager.decline(player.getUniqueId());
                player.sendMessage(Component.text("Invitación rechazada.", NamedTextColor.GRAY));
            }
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "info", "gui", "config", "buff" -> new TeamHubGUI(player, teamManager, chatPromptManager).open();
            case "ping" -> handlePing(player, args);
            case "waypoint" -> handleWaypoint(player, args);
            case "chat" -> handleChat(player);
            case "queue" -> handleQueue(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(
                "Uso: /team <invite|accept|decline|leave|kick|info|ping|waypoint|chat|queue> [args]",
                NamedTextColor.YELLOW));
    }

    private void handleInvite(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /team invite <jugador>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return;
        }

        var result = teamManager.invite(player, target);

        switch (result) {
            case OK -> {
                player.sendMessage(Component.text("✔ Invitación enviada a " + target.getName() + ".",
                        NamedTextColor.GREEN));
                target.sendMessage(Component.text(player.getName() + " te invitó a su equipo — /team accept (60s)",
                        NamedTextColor.YELLOW));
            }
            case NOT_ALLOWED -> player.sendMessage(Component.text("No tenés permiso para invitar.",
                    NamedTextColor.RED));
            case ALREADY_TEAMMATE -> player.sendMessage(Component.text("Ya está en tu equipo.", NamedTextColor.RED));
            case TARGET_IN_TEAM -> player.sendMessage(Component.text("Ese jugador ya está en un equipo.",
                    NamedTextColor.RED));
            case TEAM_FULL -> player.sendMessage(Component.text("Tu equipo está lleno.", NamedTextColor.RED));
        }
    }

    private void handleAccept(Player player) {

        var result = teamManager.accept(player);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Te uniste al equipo.", NamedTextColor.GREEN));
            case NO_INVITE -> player.sendMessage(Component.text("No tenés ninguna invitación pendiente.",
                    NamedTextColor.RED));
            case EXPIRED -> player.sendMessage(Component.text("Esa invitación ya expiró.", NamedTextColor.RED));
            case TEAM_GONE -> player.sendMessage(Component.text("Ese equipo ya no existe.", NamedTextColor.RED));
            case TEAM_FULL -> player.sendMessage(Component.text("Ese equipo ya está lleno.", NamedTextColor.RED));
        }
    }

    private void handleLeave(Player player) {
        boolean disbanded = teamManager.leave(player.getUniqueId());
        player.sendMessage(Component.text(disbanded ? "Equipo disuelto." : "Saliste del equipo.", NamedTextColor.GRAY));
    }

    private void handleKick(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /team kick <jugador>", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado.", NamedTextColor.RED));
            return;
        }

        var result = teamManager.kick(player, target.getUniqueId());
        player.sendMessage(Component.text("Resultado: " + result, NamedTextColor.GRAY));
    }

    private void handlePing(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /team ping <enemigo|objetivo|loot|npc|lugar> [etiqueta]",
                    NamedTextColor.YELLOW));
            return;
        }

        PingType type = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "enemigo", "enemy" -> PingType.ENEMY;
            case "objetivo", "objective" -> PingType.OBJECTIVE;
            case "loot", "botin" -> PingType.LOOT;
            case "npc" -> PingType.NPC;
            default -> PingType.PLACE;
        };

        String label = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";

        RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), 40, entity -> !entity.equals(player));
        Entity target = trace != null ? trace.getHitEntity() : null;

        if (target != null) {
            pingManager.ping(player, type, target.getLocation(), target, label);
            return;
        }

        RayTraceResult blockTrace = player.rayTraceBlocks(40);
        var location = blockTrace != null && blockTrace.getHitPosition() != null
                ? blockTrace.getHitPosition().toLocation(player.getWorld())
                : player.getLocation();

        pingManager.ping(player, type, location, null, label);
    }

    private void handleWaypoint(Player player, String[] args) {

        Team team = teamManager.getTeam(player.getUniqueId()).orElse(null);

        if (team == null) {
            player.sendMessage(Component.text("No estás en ningún equipo.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /team waypoint <set|list|tp|remove> [nombre]",
                    NamedTextColor.YELLOW));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Uso: /team waypoint set <nombre>", NamedTextColor.YELLOW));
                    return;
                }
                var loc = player.getLocation();
                team.addWaypoint(new com.sack.rpgroll.guilds.team.TeamWaypoint(args[2], loc.getWorld().getName(),
                        loc.getX(), loc.getY(), loc.getZ(), System.currentTimeMillis()));
                player.sendMessage(Component.text("✔ Waypoint '" + args[2] + "' guardado.", NamedTextColor.GREEN));
            }
            case "list" -> {
                player.sendMessage(Component.text("=== Waypoints del equipo ===", NamedTextColor.GOLD));
                team.waypoints().values().forEach(waypoint -> player.sendMessage(
                        Component.text(" - " + waypoint.name() + " (" + waypoint.world() + ")", NamedTextColor.GRAY)));
            }
            case "tp" -> {
                if (args.length < 3) {
                    return;
                }
                var waypoint = team.waypoints().get(args[2].toLowerCase(Locale.ROOT));
                if (waypoint == null) {
                    player.sendMessage(Component.text("No existe ese waypoint.", NamedTextColor.RED));
                    return;
                }
                var world = Bukkit.getWorld(waypoint.world());
                if (world == null) {
                    return;
                }
                player.teleport(new org.bukkit.Location(world, waypoint.x(), waypoint.y(), waypoint.z()));
            }
            case "remove" -> {
                if (args.length < 3) {
                    return;
                }
                team.removeWaypoint(args[2]);
                player.sendMessage(Component.text("Waypoint eliminado.", NamedTextColor.GRAY));
            }
            default -> player.sendMessage(Component.text("Uso: /team waypoint <set|list|tp|remove> [nombre]",
                    NamedTextColor.YELLOW));
        }
    }

    private void handleChat(Player player) {
        boolean enabled = chatListener.toggle(player);
        player.sendMessage(Component.text("Chat de equipo " + (enabled ? "activado" : "desactivado") + ".",
                NamedTextColor.AQUA));
    }

    private void handleQueue(Player player, String[] args) {

        if (args.length < 2 || args[1].equalsIgnoreCase("leave")) {
            matchmakingQueue.cancel(player.getUniqueId());
            player.sendMessage(Component.text("Saliste de la cola de emparejamiento.", NamedTextColor.GRAY));
            return;
        }

        int level = 1;
        if (RPGRollAPI.isReady()) {
            level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rpgPlayer -> rpgPlayer.getLevel()).orElse(1);
        }

        String dungeonId = args.length > 2 ? args[2] : null;

        matchmakingQueue.enqueue(new MatchmakingRequest(player.getUniqueId(), level, null, null, dungeonId, null, 4,
                System.currentTimeMillis()));

        player.sendMessage(Component.text("✔ Ingresaste a la cola de emparejamiento" + (dungeonId != null
                ? " para " + dungeonId : "") + ".", NamedTextColor.GREEN));

        for (Team formed : matchmakingQueue.tryMatch()) {
            for (var memberId : formed.members()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(Component.text("✔ Equipo formado automáticamente (" + formed.size()
                            + " jugadores).", NamedTextColor.GREEN));
                }
            }
        }
    }

}
