package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;

import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver nivel y experiencia del jugador.
 * Uso: /rpg level
 */
public class LevelCommand implements RPGCommand {

    private final RPGRoll plugin;

    public LevelCommand(RPGRoll plugin) {
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

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(player, "level_command.not_found");
                lang.send(player, "error.load_data");
                return;
            }

            displayLevel(lang, player, rpgPlayer.get());

        } catch (Exception exception) {

            lang.send(player, "level_command.error");
            exception.printStackTrace();

        }

    }

    private void displayLevel(LangManager lang, Player player, RPGPlayer rpgPlayer) {

        var progression = rpgPlayer.getProgression();

        int level = progression.level();
        int experience = progression.experience();
        int requiredExp = progression.getRequiredExpForNextLevel();
        int remaining = progression.getExpToNextLevel();

        lang.send(player, "level_command.header");
        lang.send(player, "level_command.level", "level", level);
        lang.send(player, "level_command.experience", "current", experience, "required", requiredExp);

        if (level < 100) {
            lang.send(player, "level_command.next_level", "remaining", remaining);
        } else {
            lang.send(player, "level_command.max_level");
        }

        lang.send(player, "level_command.footer");

    }

    @Override
    public String getName() {
        return "level";
    }

    @Override
    public String getDescription() {
        return "Muestra tu nivel y experiencia";
    }

    @Override
    public String getUsage() {
        return "/rpg level";
    }

    @Override
    public List<String> getAliases() {
        return List.of("nivel", "lvl", "exp");
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.level";
    }

}
