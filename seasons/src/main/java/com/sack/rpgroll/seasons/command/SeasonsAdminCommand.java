package com.sack.rpgroll.seasons.command;

import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;
import com.sack.rpgroll.seasons.gui.ChatPromptManager;
import com.sack.rpgroll.seasons.gui.SeasonsBrowserGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /seasonsadmin browser
 * /seasonsadmin reload
 * /seasonsadmin setseason <mundo> <id>
 * /seasonsadmin advance <mundo>
 * /seasonsadmin trigger <id> <mundo>
 */
public class SeasonsAdminCommand implements CommandExecutor {

    private final CalendarManager calendarManager;
    private final SeasonManager seasonManager;
    private final WorldEventManager worldEventManager;
    private final SeasonRegionManager regionManager;
    private final WorldEventEngine worldEventEngine;
    private final com.sack.rpgroll.seasons.api.SeasonsAPI api;
    private final ChatPromptManager chatPromptManager;

    public SeasonsAdminCommand(CalendarManager calendarManager, SeasonManager seasonManager,
            WorldEventManager worldEventManager, SeasonRegionManager regionManager,
            WorldEventEngine worldEventEngine, com.sack.rpgroll.seasons.api.SeasonsAPI api,
            ChatPromptManager chatPromptManager) {
        this.calendarManager = calendarManager;
        this.seasonManager = seasonManager;
        this.worldEventManager = worldEventManager;
        this.regionManager = regionManager;
        this.worldEventEngine = worldEventEngine;
        this.api = api;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollseasons.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
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
        sender.sendMessage(Component.text(
                "Uso: /seasonsadmin <browser|reload|setseason <mundo> <id>|advance <mundo>|trigger <id> <mundo>>",
                NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el Season Studio.", NamedTextColor.RED));
            return;
        }

        new SeasonsBrowserGUI(player, calendarManager, seasonManager, worldEventManager, worldEventEngine,
                regionManager, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {

        calendarManager.reload();
        seasonManager.reload();
        worldEventManager.reload();
        regionManager.reload();

        sender.sendMessage(Component.text("✔ Recargado: " + calendarManager.count() + " calendario(s), "
                + seasonManager.count() + " estación(es), " + worldEventManager.count() + " evento(s), "
                + regionManager.count() + " región(es).", NamedTextColor.GREEN));
    }

    private void handleSetSeason(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /seasonsadmin setseason <mundo> <id>", NamedTextColor.RED));
            return;
        }

        World world = Bukkit.getWorld(args[1]);

        if (world == null) {
            sender.sendMessage(Component.text("No existe el mundo: " + args[1], NamedTextColor.RED));
            return;
        }

        if (!api.setSeason(world, args[2])) {
            sender.sendMessage(Component.text(
                    "No se pudo — la estación no existe o no está en el calendario de ese mundo.", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("✔ Estación de " + world.getName() + " forzada a " + args[2] + ".",
                NamedTextColor.GREEN));
    }

    private void handleAdvance(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /seasonsadmin advance <mundo>", NamedTextColor.RED));
            return;
        }

        World world = Bukkit.getWorld(args[1]);

        if (world == null) {
            sender.sendMessage(Component.text("No existe el mundo: " + args[1], NamedTextColor.RED));
            return;
        }

        api.advanceSeason(world);
        sender.sendMessage(Component.text("✔ Avanzada la estación de " + world.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleTrigger(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /seasonsadmin trigger <id> <mundo>", NamedTextColor.RED));
            return;
        }

        World world = Bukkit.getWorld(args[2]);

        if (world == null) {
            sender.sendMessage(Component.text("No existe el mundo: " + args[2], NamedTextColor.RED));
            return;
        }

        if (!api.triggerWorldEvent(args[1], world)) {
            sender.sendMessage(Component.text("No existe un evento con id: " + args[1], NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("✔ Evento disparado en " + world.getName() + ".", NamedTextColor.GREEN));
    }

}
