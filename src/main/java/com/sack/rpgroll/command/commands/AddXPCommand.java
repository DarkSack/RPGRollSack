package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

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
            sender.sendMessage(NamedTextColor.RED + "Uso: /rpg addxp <jugador> <cantidad>");
            return;
        }

        String targetName = args[0];
        String expStr = args[1];

        // Parsear cantidad de XP
        int amount;
        try {
            amount = Integer.parseInt(expStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(NamedTextColor.RED + "La cantidad de XP debe ser un número válido.");
            return;
        }

        if (amount < 0) {
            sender.sendMessage(NamedTextColor.RED + "La cantidad de XP debe ser positiva.");
            return;
        }

        // Buscar al jugador
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(NamedTextColor.RED + "Jugador no encontrado: " + targetName);
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(targetPlayer.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                sender.sendMessage(NamedTextColor.RED + "Error al cargar datos del jugador.");
                return;
            }

            // Agregar XP
            RPGPlayer updatedPlayer = rpgPlayer.get().addExperience(amount);
            playerManager.savePlayer(updatedPlayer);

            // Mensajes
            sender.sendMessage(NamedTextColor.GREEN + "✔ Se agregaron " + NamedTextColor.YELLOW + amount +
                    NamedTextColor.GREEN + " XP a " + NamedTextColor.YELLOW + targetPlayer.getName());

            targetPlayer.sendMessage(NamedTextColor.YELLOW + "Un administrador te ha agregado " +
                    NamedTextColor.WHITE + amount + NamedTextColor.YELLOW + " XP");

            targetPlayer.sendMessage(NamedTextColor.AQUA + "XP total: " + NamedTextColor.WHITE +
                    updatedPlayer.getExperience());

        } catch (Exception exception) {

            sender.sendMessage(NamedTextColor.RED + "Error al agregar experiencia.");
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
