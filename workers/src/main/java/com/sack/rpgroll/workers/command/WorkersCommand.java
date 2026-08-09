package com.sack.rpgroll.workers.command;

import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;
import com.sack.rpgroll.workers.gui.ChatPromptManager;
import com.sack.rpgroll.workers.gui.WorkerDetailGUI;
import com.sack.rpgroll.workers.integration.GuildsIntegration;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/** /workers inspect|hire|fire|list */
public class WorkersCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("inspect", "hire", "fire", "list");

    private final WorkerManager workerManager;
    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;

    public WorkersCommand(WorkerManager workerManager, ProfessionManager professionManager,
            ChatPromptManager chatPromptManager) {
        this.workerManager = workerManager;
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "inspect" -> handleInspect(player);
            case "hire" -> handleHire(player);
            case "fire" -> handleFire(player);
            case "list" -> handleList(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Uso: /workers <inspect|hire|fire|list>", NamedTextColor.RED));
    }

    private void handleInspect(Player player) {
        targetWorker(player).ifPresentOrElse(
                worker -> new WorkerDetailGUI(player, worker, workerManager, professionManager, chatPromptManager,
                        player::closeInventory).open(),
                () -> player.sendMessage(Component.text("Mirá directamente a un worker rastreado.", NamedTextColor.RED)));
    }

    private void handleHire(Player player) {

        targetWorker(player).ifPresentOrElse(worker -> {

            if (worker.isEmployed()) {
                player.sendMessage(Component.text("Ya tiene un empleador.", NamedTextColor.RED));
                return;
            }

            Profession profession = professionManager.get(worker.professionId()).orElse(null);
            double wage = profession != null ? profession.wageAmount() : 0;
            WageType wageType = profession != null ? profession.wageType() : WageType.PER_TASK;

            worker.hire(player.getUniqueId(), wage, wageType);
            workerManager.save(worker);
            player.sendMessage(Component.text("✔ Contratado.", NamedTextColor.GREEN));

        }, () -> player.sendMessage(Component.text("Mirá directamente a un worker rastreado.", NamedTextColor.RED)));
    }

    private void handleFire(Player player) {

        targetWorker(player).ifPresentOrElse(worker -> {

            boolean canManage = worker.employerId() != null && (worker.employerId().equals(player.getUniqueId())
                    || GuildsIntegration.sameGuild(worker.employerId(), player.getUniqueId()));

            if (!canManage) {
                player.sendMessage(Component.text("No podés despedir el worker de otro jugador.", NamedTextColor.RED));
                return;
            }

            worker.fire();
            workerManager.save(worker);
            player.sendMessage(Component.text("Despedido.", NamedTextColor.YELLOW));

        }, () -> player.sendMessage(Component.text("Mirá directamente a un worker rastreado.", NamedTextColor.RED)));
    }

    private void handleList(Player player) {

        var employed = workerManager.getAll().stream().filter(w -> player.getUniqueId().equals(w.employerId())).toList();

        player.sendMessage(Component.text("=== Tus trabajadores (" + employed.size() + ") ===", NamedTextColor.GOLD));

        for (Worker worker : employed) {
            Profession profession = professionManager.get(worker.professionId()).orElse(null);
            player.sendMessage(Component.text("- " + (profession != null ? profession.displayName() : worker.professionId())
                    + " #" + worker.id().toString().substring(0, 8), NamedTextColor.WHITE));
        }
    }

    private java.util.Optional<Worker> targetWorker(Player player) {
        Entity target = player.getTargetEntity(6);
        return target != null ? workerManager.resolve(target) : java.util.Optional.empty();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        return List.of();
    }

}
