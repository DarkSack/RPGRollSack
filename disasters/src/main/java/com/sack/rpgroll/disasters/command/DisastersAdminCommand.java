package com.sack.rpgroll.disasters.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.disasters.DisastersPlugin;
import com.sack.rpgroll.disasters.core.Disaster;
import com.sack.rpgroll.disasters.core.DisasterManager;
import com.sack.rpgroll.disasters.engine.DisasterEngine;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /disastersadmin start <id> [mundo] [nivel]
 * /disastersadmin stop [mundo]
 * /disastersadmin list
 * /disastersadmin regenerate
 * /disastersadmin reload
 */
public class DisastersAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "list", "regenerate", "reload");

    private final DisastersPlugin plugin;
    private final LangManager lang;
    private final DisasterManager disasters;
    private final DisasterEngine engine;

    public DisastersAdminCommand(DisastersPlugin plugin, LangManager lang, DisasterManager disasters,
            DisasterEngine engine) {
        this.plugin = plugin;
        this.lang = lang;
        this.disasters = disasters;
        this.engine = engine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrolldisasters.admin")) {
            lang.send(sender, "common.no_permission");
            return true;
        }

        switch (args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args);
            case "stop" -> {
                World world = worldArg(sender, args, 1);
                if (world != null) {
                    lang.send(sender, engine.stop(world) ? "command.stopped" : "command.none", "world", world.getName());
                }
            }
            case "list" -> {
                lang.send(sender, "command.list", "count", disasters.count(), "pending", engine.pendingRegeneration());
                for (Disaster disaster : disasters.getAll()) {
                    lang.send(sender, "command.list_entry", "id", disaster.id(), "disaster", disaster.displayName(),
                            "chance", String.format(Locale.ROOT, "%.1f", disaster.dailyChance() * 100),
                            "levels", levels(disaster));
                }
            }
            case "regenerate" -> lang.send(sender, "command.regenerated", "count", engine.regenerateNow());
            case "reload" -> {
                plugin.reloadAll();
                lang.send(sender, "command.reloaded", "disasters", disasters.count());
            }
            default -> lang.send(sender, "command.usage");
        }
        return true;
    }

    private void start(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.usage");
            return;
        }

        Disaster disaster = disasters.get(args[1]).orElse(null);
        if (disaster == null) {
            lang.send(sender, "command.unknown_disaster", "id", args[1]);
            return;
        }

        World world = worldArg(sender, args, 2);
        if (world == null) {
            return;
        }

        int level = engine.randomLevel(disaster);
        if (args.length > 3) {
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                level = -1;
            }
            if (!engine.levelsOf(disaster).containsKey(level)) {
                lang.send(sender, "command.invalid_level", "value", args[3], "levels", levels(disaster));
                return;
            }
        }

        if (!engine.start(disaster, world, level)) {
            lang.send(sender, "command.busy", "world", world.getName());
            return;
        }

        int seconds = disaster.warningSeconds() >= 0 ? disaster.warningSeconds()
                : plugin.getConfig().getInt("warning-seconds", 10);
        lang.send(sender, "command.started", "disaster", disaster.displayName(), "world", world.getName(),
                "level", level, "seconds", seconds);
    }

    private String levels(Disaster disaster) {
        return engine.levelsOf(disaster).keySet().stream().sorted().map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    /** El mundo de {@code args[index]}, o el del jugador que lo escribe si no lo pone. Avisa y da null si no hay. */
    private World worldArg(CommandSender sender, String[] args, int index) {

        if (args.length > index) {
            World world = Bukkit.getWorld(args[index]);
            if (world == null) {
                lang.send(sender, "command.unknown_world", "world", args[index]);
            }
            return world;
        }

        if (Senders.asPlayer(sender) instanceof Player player) {
            return player.getWorld();
        }

        lang.send(sender, "command.usage");
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("stop") && args.length == 2) {
            return TabCompleteUtil.worldNames(args[1]);
        }
        if (!sub.equals("start")) {
            return List.of();
        }
        return switch (args.length) {
            case 2 -> TabCompleteUtil.filter(args[1], disasters.getAll().stream().map(Disaster::id).toList());
            case 3 -> TabCompleteUtil.worldNames(args[2]);
            case 4 -> disasters.get(args[1])
                    .map(disaster -> TabCompleteUtil.filter(args[3],
                            engine.levelsOf(disaster).keySet().stream().sorted().map(String::valueOf).toList()))
                    .orElse(List.of());
            default -> List.of();
        };
    }

}
