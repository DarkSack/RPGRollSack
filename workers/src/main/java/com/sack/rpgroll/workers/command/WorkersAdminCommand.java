package com.sack.rpgroll.workers.command;

import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.logistics.WarehouseManager;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.workers.core.skill.SkillManager;
import com.sack.rpgroll.workers.core.worker.PersonalityTrait;
import com.sack.rpgroll.workers.core.worker.WorkerManager;
import com.sack.rpgroll.workers.gui.ChatPromptManager;
import com.sack.rpgroll.workers.gui.WorkerHubGUI;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/** /workersadmin browser|reload|spawn <profesion> [nombre] */
public class WorkersAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "spawn", "designator");

    private final ProfessionManager professionManager;
    private final SkillManager skillManager;
    private final ScheduleManager scheduleManager;
    private final WorkerEventManager workerEventManager;
    private final WorkerManager workerManager;
    private final WarehouseManager warehouseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onReload;

    public WorkersAdminCommand(ProfessionManager professionManager, SkillManager skillManager,
            ScheduleManager scheduleManager, WorkerEventManager workerEventManager, WorkerManager workerManager,
            WarehouseManager warehouseManager, ChatPromptManager chatPromptManager, Runnable onReload) {
        this.professionManager = professionManager;
        this.skillManager = skillManager;
        this.scheduleManager = scheduleManager;
        this.workerEventManager = workerEventManager;
        this.workerManager = workerManager;
        this.warehouseManager = warehouseManager;
        this.chatPromptManager = chatPromptManager;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollworkers.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "designator" -> handleDesignator(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el Worker Studio.", NamedTextColor.RED));
            return;
        }

        new WorkerHubGUI(player, professionManager, skillManager, scheduleManager, workerEventManager, workerManager,
                chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {
        onReload.run();
        sender.sendMessage(Component.text("✔ RPGRoll-Workers recargado.", NamedTextColor.GREEN));
    }

    private void handleSpawn(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede spawnear un worker.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /workersadmin spawn <profesion> [nombre]", NamedTextColor.RED));
            return;
        }

        Profession profession = professionManager.get(args[1].toLowerCase()).orElse(null);

        if (profession == null) {
            player.sendMessage(Component.text("No existe la profesión '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();
        var entityType = workerManager.resolveEntityType(profession);
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

        var worker = workerManager.spawn(entity, profession, workerManager.randomPersonality());

        if (args.length >= 3) {
            worker.setCustomName(args[2]);
            workerManager.save(worker);
        }

        player.sendMessage(Component.text("✔ Spawneado un/a " + profession.displayName() + " (" + worker.personality()
                + ").", NamedTextColor.GREEN));
    }

    private void handleDesignator(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede recibir el Designador de Almacén.", NamedTextColor.RED));
            return;
        }

        player.getInventory().addItem(com.sack.rpgroll.workers.item.WorkerItemFactory.createWarehouseDesignator());
        player.sendMessage(Component.text("✔ Te diste el Designador de Almacén.", NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /workersadmin <browser|reload|spawn <profesion> [nombre]|designator>",
                NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[1],
                    professionManager.getAll().stream().map(Profession::id).toList());
        }

        return List.of();
    }

}
