package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.log.ChatLogEntry;
import com.sack.rpgroll.chat.log.ChatLogManager;
import com.sack.rpgroll.common.lang.LangManager;

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
    private final LangManager lang;

    public ChatLogCommand(ChatLogManager logManager, LangManager lang) {
        this.logManager = logManager;
        this.lang = lang;
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
        lang.send(sender, "log.usage");
    }

    private String arg(String[] args, int index) {
        return args.length > index && !args[index].equalsIgnoreCase("-") ? args[index] : null;
    }

    private void handleSearch(CommandSender sender, String[] args) {

        String channelId = arg(args, 1);
        String player = arg(args, 2);
        String date = arg(args, 3);

        List<ChatLogEntry> results = logManager.search(channelId, player, date);

        lang.send(sender, "log.results_header", "count", results.size());

        results.stream().skip(Math.max(0, results.size() - 20)).forEach(entry ->
                sender.sendMessage(Component.text(entry.formatLine(), NamedTextColor.GRAY)));
    }

    private void handleExport(CommandSender sender, String[] args) {

        String channelId = arg(args, 1);
        String player = arg(args, 2);
        String date = arg(args, 3);

        List<ChatLogEntry> results = logManager.search(channelId, player, date);
        var file = logManager.export(results, sender.getName());

        lang.send(sender, "log.exported", "count", results.size(), "file", file.getName());
    }

    private void handleClear(CommandSender sender) {
        var file = logManager.clearRecent();
        lang.send(sender, "log.cleared", "file", file.getName());
    }

}
