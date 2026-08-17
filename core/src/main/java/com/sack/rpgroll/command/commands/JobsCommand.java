package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gui.job.JobsGUI;
import com.sack.rpgroll.player.PlayerManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para ver y gestionar los trabajos del jugador.
 * Uso: /rpg jobs
 */
public class JobsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public JobsCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            JobManager jobManager = plugin.getBootstrap().getServices().get(JobManager.class);
            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);

            JobsGUI gui = new JobsGUI(player, jobManager, playerManager, lang);
            gui.open();

        } catch (Exception exception) {
            lang.send(player, "jobs_command.error");
            plugin.getLogger().severe("✘ Error en /rpg jobs: " + exception.getMessage());
        }

    }

    @Override
    public String getName() {
        return "jobs";
    }

    @Override
    public String getDescription() {
        return "Ver y gestionar tus trabajos";
    }

    @Override
    public String getUsage() {
        return "/rpg jobs";
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.jobs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("trabajos", "trabajo");
    }

}
