package com.sack.rpgroll.quests.command;

import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.engine.QuestEngine;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public QuestCommand(QuestEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede usar este comando.");
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
        player.sendMessage(Component.text(
                "Uso: /quest <list|info|start|abandon|active|completed> [id]", NamedTextColor.RED));
    }

    private void handleList(Player player) {

        var quests = engine.getQuestManager().getAll();

        if (quests.isEmpty()) {
            player.sendMessage(Component.text("No hay misiones definidas.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Misiones disponibles:", NamedTextColor.GOLD));

        for (Quest quest : quests) {
            player.sendMessage(Component.text(
                    "• " + quest.id() + " — " + quest.displayName() + " (" + quest.category() + ", "
                            + quest.difficulty() + ")",
                    NamedTextColor.WHITE));
        }
    }

    private void handleInfo(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest info <id>", NamedTextColor.RED));
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[1]);

        if (questOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe una misión con id: " + args[1], NamedTextColor.RED));
            return;
        }

        Quest quest = questOpt.get();

        player.sendMessage(Component.text(quest.displayName(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Categoría: " + quest.category(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Dificultad: " + quest.difficulty(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Etapas: " + quest.stages().size(), NamedTextColor.GRAY));
        player.sendMessage(Component.text(
                "Repetible: " + (quest.repeatable() ? "sí" : "no"), NamedTextColor.GRAY));
    }

    private void handleStart(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest start <id>", NamedTextColor.RED));
            return;
        }

        Optional<Quest> questOpt = engine.getQuestManager().get(args[1]);

        if (questOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe una misión con id: " + args[1], NamedTextColor.RED));
            return;
        }

        var reasons = engine.startQuest(player, questOpt.get());

        if (reasons.isEmpty()) {
            player.sendMessage(Component.text("✔ Misión iniciada: " + questOpt.get().displayName(),
                    NamedTextColor.GREEN));
            return;
        }

        player.sendMessage(Component.text("No podés iniciar esta misión:", NamedTextColor.RED));
        reasons.forEach(reason -> player.sendMessage(Component.text("- " + reason, NamedTextColor.RED)));
    }

    private void handleAbandon(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /quest abandon <id>", NamedTextColor.RED));
            return;
        }

        if (engine.abandonQuest(player, args[1])) {
            player.sendMessage(Component.text("✔ Misión abandonada.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No tenés esa misión activa.", NamedTextColor.RED));
        }
    }

    private void handleActive(Player player) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.allActive().isEmpty()) {
            player.sendMessage(Component.text("No tenés misiones activas.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Misiones activas:", NamedTextColor.GOLD));

        for (ActiveQuestProgress progress : state.allActive().values()) {

            engine.getQuestManager().get(progress.questId()).ifPresent(quest -> {

                var stageOpt = quest.stageAt(progress.stageIndex());
                String stageId = stageOpt.map(s -> s.id()).orElse("?");
                int totalStages = quest.stages().size();

                player.sendMessage(Component.text(
                        "• " + quest.displayName() + " — stage " + (progress.stageIndex() + 1) + "/" + totalStages
                                + " (" + stageId + ")",
                        NamedTextColor.WHITE));
            });
        }
    }

    private void handleCompleted(Player player) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.allCompleted().isEmpty()) {
            player.sendMessage(Component.text("Todavía no completaste ninguna misión.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Misiones completadas: " + state.totalCompletedCount(),
                NamedTextColor.GOLD));

        for (String questId : state.allCompleted().keySet()) {
            player.sendMessage(Component.text("• " + questId, NamedTextColor.WHITE));
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
