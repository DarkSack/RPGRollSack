package com.sack.rpgroll.quests.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.quests.QuestsPlugin;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.gui.ChatPromptManager;
import com.sack.rpgroll.quests.gui.QuestBrowserGUI;
import com.sack.rpgroll.quests.gui.RegionBrowserGUI;
import com.sack.rpgroll.quests.region.RegionManager;
import com.sack.rpgroll.util.TabCompleteUtil;

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

    private final QuestsPlugin plugin;
    private final QuestEngine engine;
    private final RegionManager regionManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public QuestAdminCommand(QuestsPlugin plugin, QuestEngine engine, RegionManager regionManager,
            ChatPromptManager chatPromptManager, LangManager lang) {
        this.plugin = plugin;
        this.engine = engine;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
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
        lang.send(sender, "admin.usage");
    }

    private void handleBrowser(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "admin.browser_player_only");
            return;
        }

        String target = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "quests";

        if (target.equals("regions")) {
            new RegionBrowserGUI(player, regionManager, chatPromptManager, lang).open();
        } else {
            new QuestBrowserGUI(player, engine.getQuestManager(), chatPromptManager, lang).open();
        }
    }

    private void handleGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.give_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[2]);
        if (questOpt.isEmpty()) {
            lang.send(sender, "admin.quest_not_found", "id", args[2]);
            return;
        }

        engine.forceStartQuest(target, questOpt.get());
        lang.send(sender, "admin.give_success", "quest", args[2], "player", target.getName());
    }

    private void handleComplete(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.complete_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        if (engine.forceCompleteQuest(target, args[2])) {
            lang.send(sender, "admin.complete_success", "quest", args[2], "player", target.getName());
        } else {
            lang.send(sender, "admin.complete_not_active", "player", target.getName());
        }
    }

    private void handleFail(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.fail_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[2]);
        if (questOpt.isEmpty()) {
            lang.send(sender, "admin.quest_not_found", "id", args[2]);
            return;
        }

        engine.failQuest(target, questOpt.get());
        lang.send(sender, "admin.fail_success", "quest", args[2], "player", target.getName());
    }

    private void handleReset(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.reset_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        engine.getStateManager().getOrLoad(target).resetQuest(args[2]);
        lang.send(sender, "admin.reset_success", "quest", args[2], "player", target.getName());
    }

    private void handleReload(CommandSender sender) {

        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));

        engine.getQuestManager().reload();
        lang.send(sender, "admin.reload_success", "count", engine.getQuestManager().count());
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
