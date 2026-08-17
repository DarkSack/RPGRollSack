package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.language.PlayerLanguageState;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /language list|learn|speak &lt;idioma&gt; */
public class LanguageCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "learn", "speak");

    private final LanguageManager languageManager;
    private final LanguageService languageService;
    private final LangManager lang;

    public LanguageCommand(LanguageManager languageManager, LanguageService languageService, LangManager lang) {
        this.languageManager = languageManager;
        this.languageService = languageService;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        if (args.length < 1) {
            handleList(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "learn" -> handleLearn(player, args);
            case "speak" -> handleSpeak(player, args);
            default -> handleList(player);
        }

        return true;
    }

    private void handleList(Player player) {

        PlayerLanguageState state = languageService.resolve(player);

        lang.send(player, "language.list_header");

        for (var language : languageManager.getAll()) {
            boolean known = state.knows(language.id());
            boolean speaking = language.id().equalsIgnoreCase(state.speakingLanguageId());
            String status = lang.raw(known ? "language.status_known" : "language.status_unknown")
                    + (speaking ? lang.raw("language.status_speaking_suffix") : "");
            player.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(language.displayName()))
                    .append(Component.text(" (" + status + ")", NamedTextColor.GRAY)));
        }
    }

    private void handleLearn(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "language.usage_learn");
            return;
        }

        var result = languageService.learn(player, args[1]);

        switch (result) {
            case OK -> lang.send(player, "language.learned", "id", args[1]);
            case ALREADY_KNOWN -> lang.send(player, "language.already_known");
            case NOT_FOUND -> lang.send(player, "language.not_found");
        }
    }

    private void handleSpeak(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "language.usage_speak");
            return;
        }

        boolean ok = languageService.setSpeaking(player, args[1]);

        if (ok) {
            lang.send(player, "language.now_speaking", "id", args[1]);
        } else {
            lang.send(player, "language.dont_know");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && List.of("learn", "speak").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> ids = languageManager.getAll().stream().map(language -> language.id()).toList();
            return TabCompleteUtil.filter(args[1], ids);
        }

        return List.of();
    }

}
