package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.ChatColor;
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
                player.sendMessage(ChatColor.RED + "Error al cargar tus datos.");
                return;
            }

            displayLevel(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(ChatColor.RED + "Error al cargar información de nivel.");
            exception.printStackTrace();

        }

    }

    private void displayLevel(Player player, RPGPlayer rpgPlayer) {

        var progression = rpgPlayer.getProgression();

        int level = progression.level();
        int experience = progression.experience();
        int requiredExp = progression.getRequiredExpForNextLevel();
        int remaining = progression.getExpToNextLevel();

        player.sendMessage(ChatColor.GOLD + "========== Tu Nivel ==========");
        player.sendMessage(ChatColor.GREEN + "Nivel: " + ChatColor.WHITE + level);
        player.sendMessage(ChatColor.AQUA + "Experiencia: " + ChatColor.WHITE + experience + "/" + requiredExp);

        if (level < 100) {
            player.sendMessage(ChatColor.YELLOW + "Para siguiente nivel: " + ChatColor.WHITE + remaining + " EXP");

            // Barra de progreso
            String progressBar = createProgressBar(experience, requiredExp);
            player.sendMessage(ChatColor.GRAY + "[" + progressBar + ChatColor.GRAY + "]");
        } else {
            player.sendMessage(ChatColor.GOLD + "¡Has alcanzado el nivel máximo!");
        }

        player.sendMessage(ChatColor.GOLD + "==============================");

    }

    private String createProgressBar(int current, int required) {

        int barLength = 20;
        double percentage = (double) current / required;
        int filled = (int) (barLength * percentage);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(ChatColor.GREEN).append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("█");
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
