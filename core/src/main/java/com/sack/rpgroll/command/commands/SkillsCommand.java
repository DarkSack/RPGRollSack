package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.skill.Skill;
import com.sack.rpgroll.gameplay.skill.SkillManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver las habilidades del jugador.
 * Uso: /rpg skills
 */
public class SkillsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public SkillsCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            SkillManager skillManager = plugin.getBootstrap()
                    .getServices()
                    .get(SkillManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(player, "skills_command.profile_load_error");
                lang.send(player, "skills_command.data_load_error");
                return;
            }

            displaySkills(player, rpgPlayer.get(), skillManager, lang);

        } catch (Exception exception) {

            lang.send(player, "skills_command.load_error");
            exception.printStackTrace();

        }

    }

    private void displaySkills(Player player, RPGPlayer rpgPlayer, SkillManager skillManager, LangManager lang) {

        var skills = rpgPlayer.getSkills();

        player.sendMessage("");
        lang.send(player, "skills_command.header");

        if (skills.getLearnedSkillIds().isEmpty()) {
            lang.send(player, "skills_command.empty");
        } else {
            for (String skillId : skills.getLearnedSkillIds()) {

                int level = skills.getSkillLevel(skillId);
                Optional<Skill> skillOpt = skillManager.get(skillId);

                if (skillOpt.isEmpty()) {
                    lang.send(player, "skills_command.entry_unknown", "id", skillId, "level", level);
                    continue;
                }

                Skill skill = skillOpt.get();
                lang.send(player, "skills_command.entry_known",
                        "name", skill.name(),
                        "level", level,
                        "mana", skill.manaCost(),
                        "cooldown", skill.cooldownSeconds(),
                        "id", skill.id());
            }
        }

        lang.send(player, "skills_command.footer");
        player.sendMessage("");

    }

    @Override
    public String getName() {
        return "skills";
    }

    @Override
    public String getDescription() {
        return "Muestra tus habilidades aprendidas";
    }

    @Override
    public String getUsage() {
        return "/rpg skills";
    }

    @Override
    public List<String> getAliases() {
        return List.of("habilidades", "hab");
    }

}
