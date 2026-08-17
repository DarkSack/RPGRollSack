package com.sack.rpgroll.quests.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * /quest list                — misiones disponibles
 * /quest info &lt;id&gt;     — detalle de una misión
 * /quest start &lt;id&gt;    — inicia una misión
 * /quest abandon &lt;id&gt;  — abandona una misión activa
 * /quest active              — tus misiones activas y su progreso
 * /quest completed           — tus misiones completadas
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "info", "start", "abandon", "active",
            "completed");

    private final QuestEngine engine;
    private final LangManager lang;

    public QuestCommand(QuestEngine engine, LangManager lang) {
        this.engine = engine;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "general.player_only");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "start" -> handleStart(player, args);
            case "abandon" -> handleAbandon(player, args);
            case "active" -> handleActive(player);
            case "completed" -> handleCompleted(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        lang.send(player, "command.usage");
    }

    private void handleList(Player player) {

        var quests = engine.getQuestManager().getAll();

        if (quests.isEmpty()) {
            lang.send(player, "command.list_empty");
            return;
        }

        lang.send(player, "command.list_header");

        for (Quest quest : quests) {
            lang.send(player, "command.list_entry", "id", quest.id(), "name", quest.displayName(),
                    "category", quest.category(), "difficulty", quest.difficulty());
        }
    }

    private void handleInfo(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.info_usage");
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[1]);

        if (questOpt.isEmpty()) {
            lang.send(player, "command.quest_not_found", "id", args[1]);
            return;
        }

        Quest quest = questOpt.get();

        lang.send(player, "command.info_title", "name", quest.displayName());
        lang.send(player, "command.info_category", "category", quest.category());
        lang.send(player, "command.info_difficulty", "difficulty", quest.difficulty());
        lang.send(player, "command.info_stages", "count", quest.stages().size());
        lang.send(player, quest.repeatable() ? "command.info_repeatable_yes" : "command.info_repeatable_no");
    }

    private void handleStart(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.start_usage");
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[1]);

        if (questOpt.isEmpty()) {
            lang.send(player, "command.quest_not_found", "id", args[1]);
            return;
        }

        var reasons = engine.startQuest(player, questOpt.get());

        if (reasons.isEmpty()) {
            lang.send(player, "command.start_success", "name", questOpt.get().displayName());
            return;
        }

        lang.send(player, "command.start_denied_header");
        reasons.forEach(reason -> lang.send(player, "command.start_denied_reason", "reason", reason));
    }

    private void handleAbandon(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.abandon_usage");
            return;
        }

        if (engine.abandonQuest(player, args[1])) {
            lang.send(player, "command.abandon_success");
        } else {
            lang.send(player, "command.abandon_not_active");
        }
    }

    private void handleActive(Player player) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.allActive().isEmpty()) {
            lang.send(player, "command.active_empty");
            return;
        }

        lang.send(player, "command.active_header");

        for (ActiveQuestProgress progress : state.allActive().values()) {

            engine.getQuestManager().get(progress.questId()).ifPresent(quest -> {

                var stageOpt = quest.stageAt(progress.stageIndex());
                String stageId = stageOpt.map(s -> s.id()).orElse("?");
                int totalStages = quest.stages().size();

                lang.send(player, "command.active_entry", "name", quest.displayName(),
                        "current", progress.stageIndex() + 1, "total", totalStages, "stageId", stageId);
            });
        }
    }

    private void handleCompleted(Player player) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.allCompleted().isEmpty()) {
            lang.send(player, "command.completed_empty");
            return;
        }

        lang.send(player, "command.completed_header", "count", state.totalCompletedCount());

        for (String questId : state.allCompleted().keySet()) {
            lang.send(player, "command.completed_entry", "id", questId);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && List.of("info", "start", "abandon").contains(args[0].toLowerCase())) {
            List<String> questIds = engine.getQuestManager().getAll().stream().map(Quest::id).toList();
            return TabCompleteUtil.filter(args[1], questIds);
        }

        return List.of();
    }

}
