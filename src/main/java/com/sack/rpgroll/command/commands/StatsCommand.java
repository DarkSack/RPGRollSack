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
                player.sendMessage(Component.text("No se encontraron datos de tu personaje.", NamedTextColor.RED));
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                return;
            }

            displayStats(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(Component.text("Error al cargar estadísticas.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private void displayStats(Player player, RPGPlayer rpgPlayer) {

        var stats = rpgPlayer.getStats();

        player.sendMessage(Component.text("========== Tus Estadísticas ==========", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Fuerza: " + stats.strength(), NamedTextColor.RED));
        player.sendMessage(Component.text("Destreza: " + stats.dexterity(), NamedTextColor.GREEN));
        player.sendMessage(Component.text("Constitución: " + stats.constitution(), NamedTextColor.GOLD));
        player.sendMessage(Component.text("Inteligencia: " + stats.intelligence(), NamedTextColor.BLUE));
        player.sendMessage(Component.text("Sabiduría: " + stats.wisdom(), NamedTextColor.AQUA));
        player.sendMessage(Component.text("Carisma: " + stats.charisma(), NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("======================================", NamedTextColor.GOLD));

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
