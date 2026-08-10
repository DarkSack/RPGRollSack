package com.sack.rpgroll.quests.command;

import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.gui.ChatPromptManager;
import com.sack.rpgroll.quests.gui.QuestBrowserGUI;
import com.sack.rpgroll.quests.gui.RegionBrowserGUI;
import com.sack.rpgroll.quests.region.RegionManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /questadmin give &lt;jugador&gt; &lt;questId&gt;      — inicia ignorando requisitos
 * /questadmin complete &lt;jugador&gt; &lt;questId&gt;  — completa de inmediato (otorga recompensas)
 * /questadmin fail &lt;jugador&gt; &lt;questId&gt;       — marca como fallida
 * /questadmin reset &lt;jugador&gt; &lt;questId&gt;      — borra todo rastro para poder reiniciarla
 * /questadmin reload                       — recarga los YAML de quests/ y regions/
 * /questadmin browser [quests|regions]     — editor visual (crear/editar)
 */
public class QuestAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("give", "complete", "fail", "reset", "reload", "browser");

    private static final String PERMISSION = "rpgrollquests.admin.*";

    private final QuestEngine engine;
    private final RegionManager regionManager;
    private final ChatPromptManager chatPromptManager;

    public QuestAdminCommand(QuestEngine engine, RegionManager regionManager, ChatPromptManager chatPromptManager) {
        this.engine = engine;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "fail" -> handleFail(sender, args);
            case "reset" -> handleReset(sender, args);
            case "reload" -> handleReload(sender);
            case "browser" -> handleBrowser(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /questadmin <give|complete|fail|reset|reload|browser [quests|regions]> [jugador] [questId]",
                NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el navegador.", NamedTextColor.RED));
            return;
        }

        String target = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "quests";

        if (target.equals("regions")) {
            new RegionBrowserGUI(player, regionManager, chatPromptManager).open();
        } else {
            new QuestBrowserGUI(player, engine.getQuestManager(), chatPromptManager).open();
        }
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /questadmin give <jugador> <questId>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[2]);
        if (questOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe una misión con id: " + args[2], NamedTextColor.RED));
            return;
        }

        engine.forceStartQuest(target, questOpt.get());
        sender.sendMessage(Component.text("✔ Misión '" + args[2] + "' iniciada para " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleComplete(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /questadmin complete <jugador> <questId>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        if (engine.forceCompleteQuest(target, args[2])) {
            sender.sendMessage(Component.text("✔ Misión '" + args[2] + "' completada para " + target.getName(),
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text(
                    target.getName() + " no tiene esa misión activa.", NamedTextColor.RED));
        }
    }

    private void handleFail(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /questadmin fail <jugador> <questId>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[2]);
        if (questOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe una misión con id: " + args[2], NamedTextColor.RED));
            return;
        }

        engine.failQuest(target, questOpt.get());
        sender.sendMessage(Component.text("✔ Misión '" + args[2] + "' marcada como fallida para " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleReset(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /questadmin reset <jugador> <questId>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        engine.getStateManager().getOrLoad(target).resetQuest(args[2]);
        sender.sendMessage(Component.text("✔ Progreso de '" + args[2] + "' reiniciado para " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        engine.getQuestManager().reload();
        sender.sendMessage(Component.text(
                "✔ Recargado: " + engine.getQuestManager().count() + " misión(es).", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "give", "complete", "fail", "reset" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                case "browser" -> TabCompleteUtil.filter(args[1], List.of("quests", "regions"));
                default -> List.of();
            };
        }

        if (args.length == 3 && List.of("give", "complete", "fail", "reset").contains(sub)) {
            List<String> questIds = engine.getQuestManager().getAll().stream().map(Quest::id).toList();
            return TabCompleteUtil.filter(args[2], questIds);
        }

        return List.of();
    }

}
