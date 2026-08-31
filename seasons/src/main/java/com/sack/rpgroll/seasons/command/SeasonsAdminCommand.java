package com.sack.rpgroll.seasons.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.seasons.SeasonsPlugin;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;
import com.sack.rpgroll.seasons.gui.ChatPromptManager;
import com.sack.rpgroll.seasons.gui.SeasonsBrowserGUI;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /seasonsadmin browser
 * /seasonsadmin reload
 * /seasonsadmin setseason <mundo> <id>
 * /seasonsadmin advance <mundo>
 * /seasonsadmin trigger <id> <mundo>
 */
public class SeasonsAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "setseason", "advance", "trigger");

    private final SeasonsPlugin plugin;
    private final CalendarManager calendarManager;
    private final SeasonManager seasonManager;
    private final WorldEventManager worldEventManager;
    private final SeasonRegionManager regionManager;
    private final WorldEventEngine worldEventEngine;
    private final com.sack.rpgroll.seasons.api.SeasonsAPI api;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public SeasonsAdminCommand(SeasonsPlugin plugin, CalendarManager calendarManager, SeasonManager seasonManager,
            WorldEventManager worldEventManager, SeasonRegionManager regionManager,
            WorldEventEngine worldEventEngine, com.sack.rpgroll.seasons.api.SeasonsAPI api,
            ChatPromptManager chatPromptManager) {
        this.plugin = plugin;
        this.calendarManager = calendarManager;
        this.seasonManager = seasonManager;
        this.worldEventManager = worldEventManager;
        this.regionManager = regionManager;
        this.worldEventEngine = worldEventEngine;
        this.api = api;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollseasons.admin.*")) {
            lang.send(sender, "common.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "setseason" -> handleSetSeason(sender, args);
            case "advance" -> handleAdvance(sender, args);
            case "trigger" -> handleTrigger(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.admin.usage");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.admin.players_only_studio");
            return;
        }

        new SeasonsBrowserGUI(player, calendarManager, seasonManager, worldEventManager, worldEventEngine,
                regionManager, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {

        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));

        calendarManager.reload();
        seasonManager.reload();
        worldEventManager.reload();
        regionManager.reload();

        lang.send(sender, "command.admin.reloaded",
                "calendars", calendarManager.count(),
                "seasons", seasonManager.count(),
                "events", worldEventManager.count(),
                "regions", regionManager.count());
    }

    private void handleSetSeason(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.admin.usage_setseason");
            return;
        }

        World world = Bukkit.getWorld(args[1]);

        if (world == null) {
            lang.send(sender, "command.admin.unknown_world", "world", args[1]);
            return;
        }

        if (!api.setSeason(world, args[2])) {
            lang.send(sender, "command.admin.setseason_failed");
            return;
        }

        lang.send(sender, "command.admin.setseason_success", "world", world.getName(), "season", args[2]);
    }

    private void handleAdvance(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.admin.usage_advance");
            return;
        }

        World world = Bukkit.getWorld(args[1]);

        if (world == null) {
            lang.send(sender, "command.admin.unknown_world", "world", args[1]);
            return;
        }

        api.advanceSeason(world);
        lang.send(sender, "command.admin.advance_success", "world", world.getName());
    }

    private void handleTrigger(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.admin.usage_trigger");
            return;
        }

        World world = Bukkit.getWorld(args[2]);

        if (world == null) {
            lang.send(sender, "command.admin.unknown_world", "world", args[2]);
            return;
        }

        if (!api.triggerWorldEvent(args[1], world)) {
            lang.send(sender, "command.admin.unknown_event", "id", args[1]);
            return;
        }

        lang.send(sender, "command.admin.trigger_success", "world", world.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "setseason", "advance" -> TabCompleteUtil.worldNames(args[1]);
                case "trigger" -> TabCompleteUtil.filter(args[1],
                        worldEventManager.getAll().stream().map(e -> e.id()).toList());
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "setseason" -> TabCompleteUtil.filter(args[2],
                        seasonManager.getAll().stream().map(s -> s.id()).toList());
                case "trigger" -> TabCompleteUtil.worldNames(args[2]);
                default -> List.of();
            };
        }

        return List.of();
    }

}
