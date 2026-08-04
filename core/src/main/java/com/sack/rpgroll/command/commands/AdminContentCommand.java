package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gameplay.skill.SkillManager;
import com.sack.rpgroll.gameplay.trait.TraitManager;
import com.sack.rpgroll.gui.admin.ChatPromptManager;
import com.sack.rpgroll.gui.admin.ClassBrowserGUI;
import com.sack.rpgroll.gui.admin.JobBrowserGUI;
import com.sack.rpgroll.gui.admin.RaceBrowserGUI;
import com.sack.rpgroll.gui.admin.SkillBrowserGUI;
import com.sack.rpgroll.gui.admin.TraitBrowserGUI;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.playerclass.ClassManagerImpl;
import com.sack.rpgroll.race.RaceManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Editor visual de contenido base del framework: razas, clases, trabajos,
 * skills y traits. Uso: /rpg admincontent &lt;race|class|job|skill|trait&gt;
 */
public class AdminContentCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminContentCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        var services = plugin.getBootstrap().getServices();
        ChatPromptManager chatPromptManager = services.get(ChatPromptManager.class);

        switch (args[0].toLowerCase()) {
            case "race" -> new RaceBrowserGUI(player, (RaceManagerImpl) services.get(RaceManager.class),
                    chatPromptManager).open();
            case "class" -> new ClassBrowserGUI(player, (ClassManagerImpl) services.get(ClassManager.class),
                    chatPromptManager).open();
            case "job" -> new JobBrowserGUI(player, services.get(JobManager.class), chatPromptManager).open();
            case "skill" -> new SkillBrowserGUI(player, services.get(SkillManager.class), chatPromptManager).open();
            case "trait" -> new TraitBrowserGUI(player, services.get(TraitManager.class), chatPromptManager).open();
            default -> player.sendMessage(
                    Component.text("Opción inválida. Usa: race, class, job, skill o trait", NamedTextColor.RED));
        }
    }

    @Override
    public String getName() {
        return "admincontent";
    }

    @Override
    public String getDescription() {
        return "Editor visual de razas/clases/trabajos/skills/traits (crear/editar)";
    }

    @Override
    public String getUsage() {
        return "/rpg admincontent <race|class|job|skill|trait>";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.content";
    }

    @Override
    public List<String> getAliases() {
        return List.of("acontent");
    }

}
