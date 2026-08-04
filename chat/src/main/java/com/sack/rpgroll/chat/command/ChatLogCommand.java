package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.log.ChatLogEntry;
import com.sack.rpgroll.chat.log.ChatLogManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

/** /chatlog search|export|clear [canal] [jugador] [fecha] */
public class ChatLogCommand implements CommandExecutor {

    private final ChatLogManager logManager;

    public ChatLogCommand(ChatLogManager logManager) {
        this.logManager = logManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "search" -> handleSearch(sender, args);
            case "export" -> handleExport(sender, args);
            case "clear" -> handleClear(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /chatlog <search|export|clear> [canal] [jugador] [fecha yyyy-MM-dd]",
                NamedTextColor.YELLOW));
    }

    private String arg(String[] args, int index) {
        return args.length > index && !args[index].equalsIgnoreCase("-") ? args[index] : null;
    }

    private void handleSearch(CommandSender sender, String[] args) {

        String channelId = arg(args, 1);
        String player = arg(args, 2);
        String date = arg(args, 3);

        List<ChatLogEntry> results = logManager.search(channelId, player, date);

        sender.sendMessage(Component.text("=== Resultados (" + results.size() + ", últimos 20) ===",
                NamedTextColor.GOLD));

        results.stream().skip(Math.max(0, results.size() - 20)).forEach(entry ->
                sender.sendMessage(Component.text(entry.formatLine(), NamedTextColor.GRAY)));
    }

    private void handleExport(CommandSender sender, String[] args) {

        String channelId = arg(args, 1);
        String player = arg(args, 2);
        String date = arg(args, 3);

        List<ChatLogEntry> results = logManager.search(channelId, player, date);
        var file = logManager.export(results, sender.getName());

        sender.sendMessage(Component.text("✔ Exportado " + results.size() + " mensaje(s) a " + file.getName(),
                NamedTextColor.GREEN));
    }

    private void handleClear(CommandSender sender) {
        var file = logManager.clearRecent();
        sender.sendMessage(Component.text("✔ Historial en memoria limpiado (respaldo en " + file.getName() + ").",
                NamedTextColor.GREEN));
    }

}
