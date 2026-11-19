package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.log.ChatLogEntry;
import com.sack.rpgroll.chat.log.ChatLogManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

/** /chatlog search|export|clear [canal] [jugador] [fecha] */
public class ChatLogCommand implements CommandExecutor, TabCompleter {

    private final ChatLogManager logManager;
    private final LangManager lang;
    private final java.util.function.Supplier<List<String>> channelIds;

    public ChatLogCommand(ChatLogManager logManager, LangManager lang) {
        this(logManager, lang, List::of);
    }

    public ChatLogCommand(ChatLogManager logManager, LangManager lang,
            java.util.function.Supplier<List<String>> channelIds) {
        this.channelIds = channelIds;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return com.sack.rpgroll.util.TabCompleteUtil.filter(args[0], List.of("search", "export", "clear"));
        }
        if (args.length > 4 || args[0].equalsIgnoreCase("clear")) {
            return List.of();
        }
        String token = args[args.length - 1];
        List<String> options = new java.util.ArrayList<>(List.of("-"));
        switch (args.length) {
            case 2 -> options.addAll(channelIds.get());
            case 3 -> options.addAll(com.sack.rpgroll.util.TabCompleteUtil.allOnlinePlayerNames());
            default -> options.add(java.time.LocalDate.now().toString());
        }
        return com.sack.rpgroll.util.TabCompleteUtil.filter(token, options);
    }
}
