package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(
                        Component.text("No se pudo encontrar tu información de jugador.").color(NamedTextColor.RED));
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                return;
            }

            displayLevel(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(Component.text("Error al cargar información de nivel.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private void displayLevel(Player player, RPGPlayer rpgPlayer) {

        var progression = rpgPlayer.getProgression();

        int level = progression.level();
        int experience = progression.experience();
        int requiredExp = progression.getRequiredExpForNextLevel();
        int remaining = progression.getExpToNextLevel();

        player.sendMessage(Component.text("========== Tu Nivel ==========", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Nivel: " + level, NamedTextColor.GREEN));
        player.sendMessage(
                Component.text("Experiencia: " + experience + "/" + requiredExp, NamedTextColor.AQUA));

        if (level < 100) {
            player.sendMessage(
                    Component.text("Para siguiente nivel: " + remaining + " EXP", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("¡Has alcanzado el nivel máximo!", NamedTextColor.GOLD));
        }

        player.sendMessage(Component.text("==============================", NamedTextColor.GOLD));

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

}
