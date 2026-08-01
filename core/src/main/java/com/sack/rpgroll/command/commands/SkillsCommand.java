package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(Component.text("No se encontraron datos de jugador para ti.", NamedTextColor.RED));
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                return;
            }

            displaySkills(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(Component.text("Error al cargar habilidades.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private void displaySkills(Player player, RPGPlayer rpgPlayer) {

        var skills = rpgPlayer.getSkills();

        player.sendMessage("");
        player.sendMessage(Component.text("============ Tus Habilidades ===========", NamedTextColor.GOLD));

        if (skills.getLearnedSkillIds().isEmpty()) {
            player.sendMessage(Component.text("Aún no has aprendido habilidades.", NamedTextColor.YELLOW));
        } else {
            for (String skillId : skills.getLearnedSkillIds()) {
                int level = skills.getSkillLevel(skillId);
                player.sendMessage(Component.text("• " + skillId + " (Nivel " + level + ")", NamedTextColor.GREEN));
            }
        }

        player.sendMessage(Component.text("========================================", NamedTextColor.GOLD));
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
