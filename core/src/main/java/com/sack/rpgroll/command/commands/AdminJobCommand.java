package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando de administrador para gestionar trabajos de un jugador.
 * Uso:
 * /rpg admin job give <jugador> <jobId>
 * /rpg admin job remove <jugador> <jobId>
 * /rpg admin job setlevel <jugador> <jobId> <nivel>
 */
public class AdminJobCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminJobCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String action = args[0].toLowerCase();

        try {
            switch (action) {
                case "give" -> give(sender, args);
                case "remove" -> remove(sender, args);
                case "setlevel" -> setLevel(sender, args);
                default ->
                    sender.sendMessage(Component.text("Acción inválida: give, remove, setlevel", NamedTextColor.RED));
            }
        } catch (Exception exception) {
            sender.sendMessage(Component.text("Error al ejecutar el comando.", NamedTextColor.RED));
            plugin.getLogger().severe("✘ Error en /rpg admin job: " + exception.getMessage());
        }
    }

    private void give(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /rpg job give <jugador> <jobId>", NamedTextColor.RED));
            return;
        }

        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
        Optional<Job> jobOpt = jobManager.get(jobId);

        if (jobOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe el trabajo: " + jobId, NamedTextColor.RED));
            return;
        }

        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            sender.sendMessage(Component.text("El jugador no tiene datos RPG cargados.", NamedTextColor.RED));
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (rpgPlayer.getJobs().hasJob(jobId)) {
            sender.sendMessage(Component.text("El jugador ya tiene ese trabajo.", NamedTextColor.YELLOW));
            return;
        }

        if (rpgPlayer.getJobs().isFull()) {
            sender.sendMessage(Component.text(
                    "El jugador ya tiene el máximo de trabajos (3). Usa remove primero.", NamedTextColor.RED));
            return;
        }

        playerManager.savePlayer(rpgPlayer.joinJob(jobId));

        sender.sendMessage(Component.text("✔ Trabajo asignado: ", NamedTextColor.GREEN)
                .append(Component.text(jobOpt.get().displayName(), NamedTextColor.GOLD))
                .append(Component.text(" a " + target.getName(), NamedTextColor.GREEN)));
    }

    private void remove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /rpg job remove <jugador> <jobId>", NamedTextColor.RED));
            return;
        }

        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            sender.sendMessage(Component.text("El jugador no tiene datos RPG cargados.", NamedTextColor.RED));
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (!rpgPlayer.getJobs().hasJob(jobId)) {
            sender.sendMessage(Component.text("El jugador no tiene ese trabajo.", NamedTextColor.YELLOW));
            return;
        }

        playerManager.savePlayer(rpgPlayer.leaveJob(jobId));

        sender.sendMessage(Component.text("✔ Trabajo removido de " + target.getName(), NamedTextColor.GREEN));
    }

    private void setLevel(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /rpg job setlevel <jugador> <jobId> <nivel>", NamedTextColor.RED));
            return;
        }

        Player target = resolveTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        int level;

        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("El nivel debe ser un número.", NamedTextColor.RED));
            return;
        }

        JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
        Optional<Job> jobOpt = jobManager.get(jobId);

        if (jobOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe el trabajo: " + jobId, NamedTextColor.RED));
            return;
        }

        if (level < 1 || level > jobOpt.get().maxLevel()) {
            sender.sendMessage(
                    Component.text("Nivel inválido. Máximo: " + jobOpt.get().maxLevel(), NamedTextColor.RED));
            return;
        }

        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            sender.sendMessage(Component.text("El jugador no tiene datos RPG cargados.", NamedTextColor.RED));
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerJobs jobs = rpgPlayer.getJobs();

        if (!jobs.hasJob(jobId)) {
            sender.sendMessage(Component.text("El jugador no tiene ese trabajo activo.", NamedTextColor.RED));
            return;
        }

        JobProgress newProgress = new JobProgress(jobId, level, 0);
        playerManager.savePlayer(rpgPlayer.updateJobs(jobs.withProgress(newProgress)));

        sender.sendMessage(Component.text("✔ Nivel de " + jobId + " para " + target.getName()
                + " establecido en " + level, NamedTextColor.GREEN));
    }

    private Player resolveTarget(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado o desconectado: " + name, NamedTextColor.RED));
        }
        return target;
    }

    @Override
    public String getName() {
        return "job";
    }

    @Override
    public String getDescription() {
        return "Gestiona trabajos de un jugador (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg job <give|remove|setlevel> <jugador> <jobId> [nivel]";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.job";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return List.of("give", "remove", "setlevel");
        }
        if (args.length == 2) {
            return TabCompleteUtil.allOnlinePlayerNames();
        }
        if (args.length == 3) {
            JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
            return jobManager.getAll().stream().map(Job::id).toList();
        }
        return List.of();
    }

}