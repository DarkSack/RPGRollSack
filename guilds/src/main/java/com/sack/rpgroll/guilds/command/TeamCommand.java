package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.guilds.gui.team.TeamHubGUI;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.chat.TeamChatListener;
import com.sack.rpgroll.guilds.team.matchmaking.MatchmakingRequest;
import com.sack.rpgroll.guilds.team.matchmaking.TeamMatchmakingQueue;
import com.sack.rpgroll.guilds.team.ping.PingType;
import com.sack.rpgroll.guilds.team.ping.TeamPingManager;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Locale;

/** /team create|invite|accept|decline|leave|kick|promote|info|config|buff|ping|waypoint|chat|queue */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("invite", "accept", "decline", "leave", "kick", "info",
            "gui", "config", "buff", "ping", "waypoint", "chat", "queue");
    private static final List<String> PING_TYPES = List.of("enemigo", "objetivo", "loot", "npc", "lugar");
    private static final List<String> WAYPOINT_SUBCOMMANDS = List.of("set", "list", "tp", "remove");

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

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang().send(sender, "common.players_only");
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
                lang().send(player, "team.decline.success");
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
        lang().send(player, "team.usage");
    }

    private void handleInvite(Player player, String[] args) {

        if (args.length < 2) {
            lang().send(player, "team.invite.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang().send(player, "common.player_not_found");
            return;
        }

        var result = teamManager.invite(player, target);

        switch (result) {
            case OK -> {
                lang().send(player, "team.invite.sent", "player", target.getName());
                lang().send(target, "team.invite.received", "player", player.getName());
            }
            case NOT_ALLOWED -> lang().send(player, "team.invite.not_allowed");
            case ALREADY_TEAMMATE -> lang().send(player, "team.invite.already_teammate");
            case TARGET_IN_TEAM -> lang().send(player, "team.invite.target_in_team");
            case TEAM_FULL -> lang().send(player, "team.invite.team_full");
        }
    }

    private void handleAccept(Player player) {

        var result = teamManager.accept(player);

        switch (result) {
            case OK -> lang().send(player, "team.accept.success");
            case NO_INVITE -> lang().send(player, "team.accept.no_invite");
            case EXPIRED -> lang().send(player, "team.accept.expired");
            case TEAM_GONE -> lang().send(player, "team.accept.team_gone");
            case TEAM_FULL -> lang().send(player, "team.accept.team_full");
        }
    }

    private void handleLeave(Player player) {
        boolean disbanded = teamManager.leave(player.getUniqueId());
        lang().send(player, disbanded ? "team.leave.disbanded" : "team.leave.left");
    }

    private void handleKick(Player player, String[] args) {

        if (args.length < 2) {
            lang().send(player, "team.kick.usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang().send(player, "common.player_not_found");
            return;
        }

        var result = teamManager.kick(player, target.getUniqueId());
        lang().send(player, "common.result", "result", result);
    }

    private void handlePing(Player player, String[] args) {

        if (args.length < 2) {
            lang().send(player, "team.ping.usage");
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
            lang().send(player, "team.waypoint.not_in_team");
            return;
        }

        if (args.length < 2) {
            lang().send(player, "team.waypoint.usage");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 3) {
                    lang().send(player, "team.waypoint.set_usage");
                    return;
                }
                var loc = player.getLocation();
                team.addWaypoint(new com.sack.rpgroll.guilds.team.TeamWaypoint(args[2], loc.getWorld().getName(),
                        loc.getX(), loc.getY(), loc.getZ(), System.currentTimeMillis()));
                lang().send(player, "team.waypoint.set_success", "name", args[2]);
            }
            case "list" -> {
                lang().send(player, "team.waypoint.list_header");
                team.waypoints().values().forEach(waypoint -> lang().send(player, "team.waypoint.list_entry",
                        "name", waypoint.name(), "world", waypoint.world()));
            }
            case "tp" -> {
                if (args.length < 3) {
                    return;
                }
                var waypoint = team.waypoints().get(args[2].toLowerCase(Locale.ROOT));
                if (waypoint == null) {
                    lang().send(player, "team.waypoint.not_found");
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
                lang().send(player, "team.waypoint.removed");
            }
            default -> lang().send(player, "team.waypoint.usage");
        }
    }

    private void handleChat(Player player) {
        boolean enabled = chatListener.toggle(player);
        lang().send(player, enabled ? "team.chat.enabled" : "team.chat.disabled");
    }

    private void handleQueue(Player player, String[] args) {

        if (args.length < 2 || args[1].equalsIgnoreCase("leave")) {
            matchmakingQueue.cancel(player.getUniqueId());
            lang().send(player, "team.queue.left");
            return;
        }

        int level = 1;
        if (RPGRollAPI.isReady()) {
            level = RPGRollAPI.get().getPlayer(player.getUniqueId()).map(rpgPlayer -> rpgPlayer.getLevel()).orElse(1);
        }

        String dungeonId = args.length > 2 ? args[2] : null;

        matchmakingQueue.enqueue(new MatchmakingRequest(player.getUniqueId(), level, null, null, dungeonId, null, 4,
                System.currentTimeMillis()));

        if (dungeonId != null) {
            lang().send(player, "team.queue.joined_for", "dungeon", dungeonId);
        } else {
            lang().send(player, "team.queue.joined");
        }

        for (Team formed : matchmakingQueue.tryMatch()) {
            for (var memberId : formed.members()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    lang().send(member, "team.queue.formed", "size", formed.size());
                }
            }
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
                case "invite", "kick" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                case "ping" -> TabCompleteUtil.filter(args[1], PING_TYPES);
                case "waypoint" -> TabCompleteUtil.filter(args[1], WAYPOINT_SUBCOMMANDS);
                case "queue" -> TabCompleteUtil.filter(args[1], List.of("leave"));
                default -> List.of();
            };
        }

        if (args.length == 3 && "waypoint".equals(sub)
                && List.of("tp", "remove").contains(args[1].toLowerCase(Locale.ROOT))
                && sender instanceof Player player) {
            Team team = teamManager.getTeam(player.getUniqueId()).orElse(null);
            if (team == null) {
                return List.of();
            }
            return TabCompleteUtil.filter(args[2], team.waypoints().keySet());
        }

        return List.of();
    }

}
