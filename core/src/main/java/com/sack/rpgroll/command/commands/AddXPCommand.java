package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
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
            sender.sendMessage(Component.text("Uso: /rpg addxp <jugador> <cantidad>", NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        String expStr = args[1];

        // Parsear cantidad de XP
        int amount;
        try {
            amount = Integer.parseInt(expStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("La cantidad de XP debe ser un número válido.", NamedTextColor.RED));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(Component.text("La cantidad de XP debe ser positiva.", NamedTextColor.RED));
            return;
        }

        // Buscar al jugador
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + targetName, NamedTextColor.RED));
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(targetPlayer.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                sender.sendMessage(Component.text("Error al cargar datos del jugador.", NamedTextColor.RED));
                return;
            }

            // Agregar XP
            RPGPlayer updatedPlayer = rpgPlayer.get().addExperience(amount);
            playerManager.savePlayer(updatedPlayer);

            // Mensajes
            sender.sendMessage(Component.text("✔ Se agregaron " + amount + " XP a " + targetPlayer.getName(),
                    NamedTextColor.GREEN));

            targetPlayer.sendMessage(
                    Component.text("Un administrador te ha agregado " + amount + " XP", NamedTextColor.YELLOW)
                            .append(Component.text(". Tu XP total ahora es: " + updatedPlayer.getExperience(),
                                    NamedTextColor.AQUA)));

            targetPlayer.sendMessage(Component.text("XP total: " + updatedPlayer.getExperience(), NamedTextColor.AQUA)
                    .append(Component.text(" | Nivel: " + updatedPlayer.getLevel(), NamedTextColor.AQUA)));

        } catch (Exception exception) {

            sender.sendMessage(Component.text("Error al agregar experiencia.", NamedTextColor.RED));
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

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return TabCompleteUtil.allOnlinePlayerNames();
        }
        if (args.length == 2) {
            return List.of("100", "500", "1000");
        }
        return List.of();
    }

}
