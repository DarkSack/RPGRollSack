package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Comando de administrador para agregar experiencia a jugadores.
 * Uso: /rpg addxp <jugador> <cantidad>
 */
public class AddXPCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AddXPCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /rpg addxp <jugador> <cantidad>");
            return;
        }

        String targetName = args[0];
        String expStr = args[1];

        // Parsear cantidad de XP
        int amount;
        try {
            amount = Integer.parseInt(expStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La cantidad de XP debe ser un número válido.");
            return;
        }

        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "La cantidad de XP debe ser positiva.");
            return;
        }

        // Buscar al jugador
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado: " + targetName);
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(targetPlayer.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Error al cargar datos del jugador.");
                return;
            }

            // Agregar XP
            RPGPlayer updatedPlayer = rpgPlayer.get().addExperience(amount);
            playerManager.savePlayer(updatedPlayer);

            // Mensajes
            sender.sendMessage(ChatColor.GREEN + "✔ Se agregaron " + ChatColor.YELLOW + amount +
                    ChatColor.GREEN + " XP a " + ChatColor.YELLOW + targetPlayer.getName());

            targetPlayer.sendMessage(ChatColor.YELLOW + "Un administrador te ha agregado " +
                    ChatColor.WHITE + amount + ChatColor.YELLOW + " XP");

            targetPlayer.sendMessage(ChatColor.AQUA + "XP total: " + ChatColor.WHITE +
                    updatedPlayer.getExperience());

        } catch (Exception exception) {

            sender.sendMessage(ChatColor.RED + "Error al agregar experiencia.");
            exception.printStackTrace();

        }

    }

    @Override
    public String getName() {
        return "addxp";
    }

    @Override
    public String getDescription() {
        return "Agrega experiencia a un jugador (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg addxp <jugador> <cantidad>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dxp");
    }

}
