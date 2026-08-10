package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.chat.language.LanguageService;
import com.sack.rpgroll.chat.language.PlayerLanguageState;
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

    public LanguageCommand(LanguageManager languageManager, LanguageService languageService) {
        this.languageManager = languageManager;
        this.languageService = languageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
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

        player.sendMessage(Component.text("=== Idiomas ===", NamedTextColor.GOLD));

        for (var language : languageManager.getAll()) {
            boolean known = state.knows(language.id());
            boolean speaking = language.id().equalsIgnoreCase(state.speakingLanguageId());
            player.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(language.displayName()))
                    .append(Component.text(" (" + (known ? "conocido" : "desconocido")
                            + (speaking ? ", hablando" : "") + ")", NamedTextColor.GRAY)));
        }
    }

    private void handleLearn(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /language learn <idioma>", NamedTextColor.YELLOW));
            return;
        }

        var result = languageService.learn(player, args[1]);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Aprendiste " + args[1] + ".", NamedTextColor.GREEN));
            case ALREADY_KNOWN -> player.sendMessage(Component.text("Ya conocés ese idioma.", NamedTextColor.YELLOW));
            case NOT_FOUND -> player.sendMessage(Component.text("No existe ese idioma.", NamedTextColor.RED));
        }
    }

    private void handleSpeak(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /language speak <idioma>", NamedTextColor.YELLOW));
            return;
        }

        boolean ok = languageService.setSpeaking(player, args[1]);

        player.sendMessage(ok
                ? Component.text("✔ Ahora hablás en " + args[1] + ".", NamedTextColor.GREEN)
                : Component.text("No conocés ese idioma.", NamedTextColor.RED));
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
