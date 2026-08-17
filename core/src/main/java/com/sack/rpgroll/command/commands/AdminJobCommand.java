package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.util.TabCompleteUtil;

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

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 2) {
            lang.send(sender, "admin_job.usage", "usage", getUsage());
            return;
        }

        String action = args[0].toLowerCase();

        try {
            switch (action) {
                case "give" -> give(sender, args, lang);
                case "remove" -> remove(sender, args, lang);
                case "setlevel" -> setLevel(sender, args, lang);
                default -> lang.send(sender, "admin_job.invalid_action");
            }
        } catch (Exception exception) {
            lang.send(sender, "error.command_failed");
            plugin.getLogger().severe("✘ Error en /rpg admin job: " + exception.getMessage());
        }
    }

    private void give(CommandSender sender, String[] args, LangManager lang) {

        if (args.length < 3) {
            lang.send(sender, "admin_job.give_usage");
            return;
        }

        Player target = resolveTarget(sender, args[1], lang);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
        Optional<Job> jobOpt = jobManager.get(jobId);

        if (jobOpt.isEmpty()) {
            lang.send(sender, "admin_job.job_not_found", "job", jobId);
            return;
        }

        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            lang.send(sender, "error.no_rpg_data");
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (rpgPlayer.getJobs().hasJob(jobId)) {
            lang.send(sender, "admin_job.already_has_job");
            return;
        }

        if (rpgPlayer.getJobs().isFull()) {
            lang.send(sender, "admin_job.jobs_full");
            return;
        }

        playerManager.savePlayer(rpgPlayer.joinJob(jobId));

        lang.send(sender, "admin_job.give_success", "job", jobOpt.get().displayName(), "player", target.getName());
    }

    private void remove(CommandSender sender, String[] args, LangManager lang) {

        if (args.length < 3) {
            lang.send(sender, "admin_job.remove_usage");
            return;
        }

        Player target = resolveTarget(sender, args[1], lang);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            lang.send(sender, "error.no_rpg_data");
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        if (!rpgPlayer.getJobs().hasJob(jobId)) {
            lang.send(sender, "admin_job.remove_no_job");
            return;
        }

        playerManager.savePlayer(rpgPlayer.leaveJob(jobId));

        lang.send(sender, "admin_job.remove_success", "player", target.getName());
    }

    private void setLevel(CommandSender sender, String[] args, LangManager lang) {

        if (args.length < 4) {
            lang.send(sender, "admin_job.setlevel_usage");
            return;
        }

        Player target = resolveTarget(sender, args[1], lang);
        if (target == null) {
            return;
        }

        String jobId = args[2];
        int level;

        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            lang.send(sender, "admin_job.invalid_level_number");
            return;
        }

        JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
        Optional<Job> jobOpt = jobManager.get(jobId);

        if (jobOpt.isEmpty()) {
            lang.send(sender, "admin_job.job_not_found", "job", jobId);
            return;
        }

        if (level < 1 || level > jobOpt.get().maxLevel()) {
            lang.send(sender, "admin_job.invalid_level_range", "max", jobOpt.get().maxLevel());
            return;
        }

        PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            lang.send(sender, "error.no_rpg_data");
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerJobs jobs = rpgPlayer.getJobs();

        if (!jobs.hasJob(jobId)) {
            lang.send(sender, "admin_job.no_active_job");
            return;
        }

        JobProgress newProgress = new JobProgress(jobId, level, 0);
        playerManager.savePlayer(rpgPlayer.updateJobs(jobs.withProgress(newProgress)));

        lang.send(sender, "admin_job.setlevel_success", "job", jobId, "player", target.getName(), "level", level);
    }

    private Player resolveTarget(CommandSender sender, String name, LangManager lang) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            lang.send(sender, "error.player_offline", "player", name);
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