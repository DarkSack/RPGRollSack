package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;

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
                player.sendMessage(NamedTextColor.RED + "Error al cargar tus datos.");
                return;
            }

            displayLevel(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(NamedTextColor.RED + "Error al cargar información de nivel.");
            exception.printStackTrace();

        }

    }

    private void displayLevel(Player player, RPGPlayer rpgPlayer) {

        var progression = rpgPlayer.getProgression();

        int level = progression.level();
        int experience = progression.experience();
        int requiredExp = progression.getRequiredExpForNextLevel();
        int remaining = progression.getExpToNextLevel();

        player.sendMessage(NamedTextColor.GOLD + "========== Tu Nivel ==========");
        player.sendMessage(NamedTextColor.GREEN + "Nivel: " + NamedTextColor.WHITE + level);
        player.sendMessage(
                NamedTextColor.AQUA + "Experiencia: " + NamedTextColor.WHITE + experience + "/" + requiredExp);

        if (level < 100) {
            player.sendMessage(
                    NamedTextColor.YELLOW + "Para siguiente nivel: " + NamedTextColor.WHITE + remaining + " EXP");

            // Barra de progreso
            String progressBar = createProgressBar(experience, requiredExp);
            player.sendMessage(NamedTextColor.GRAY + "[" + progressBar + NamedTextColor.GRAY + "]");
        } else {
            player.sendMessage(NamedTextColor.GOLD + "¡Has alcanzado el nivel máximo!");
        }

        player.sendMessage(NamedTextColor.GOLD + "==============================");

    }

    private String createProgressBar(int current, int required) {

        int barLength = 20;
        double percentage = (double) current / required;
        int filled = (int) (barLength * percentage);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(NamedTextColor.GREEN).append("█");
            } else {
                bar.append(NamedTextColor.DARK_GRAY).append("█");
            }
        }

        return bar.toString();

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
