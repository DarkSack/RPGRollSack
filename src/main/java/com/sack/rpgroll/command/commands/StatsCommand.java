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
 * Comando para ver las estadísticas del jugador.
 * Uso: /rpg stats
 */
public class StatsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public StatsCommand(RPGRoll plugin) {
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

            displayStats(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(ChatColor.RED + "Error al cargar estadísticas.");
            exception.printStackTrace();

        }

    }

    private void displayStats(Player player, RPGPlayer rpgPlayer) {

        var stats = rpgPlayer.getStats();

        player.sendMessage(ChatColor.GOLD + "========== Tus Estadísticas ==========");
        player.sendMessage(ChatColor.RED + "Fuerza: " + ChatColor.WHITE + stats.strength());
        player.sendMessage(ChatColor.GREEN + "Destreza: " + ChatColor.WHITE + stats.dexterity());
        player.sendMessage(ChatColor.GOLD + "Constitución: " + ChatColor.WHITE + stats.constitution());
        player.sendMessage(ChatColor.BLUE + "Inteligencia: " + ChatColor.WHITE + stats.intelligence());
        player.sendMessage(ChatColor.AQUA + "Sabiduría: " + ChatColor.WHITE + stats.wisdom());
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Carisma: " + ChatColor.WHITE + stats.charisma());
        player.sendMessage(ChatColor.GOLD + "======================================");

    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Muestra tus estadísticas";
    }

    @Override
    public String getUsage() {
        return "/rpg stats";
    }

    @Override
    public List<String> getAliases() {
        return List.of("estadisticas", "est");
    }

}
