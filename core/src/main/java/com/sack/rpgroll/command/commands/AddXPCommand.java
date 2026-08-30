package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.util.TabCompleteUtil;

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

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 2) {
            lang.send(sender, "addxp.usage");
            return;
        }

        String targetName = args[0];
        String expStr = args[1];

        // Parsear cantidad de XP
        int amount;
        try {
            amount = Integer.parseInt(expStr);
        } catch (NumberFormatException e) {
            lang.send(sender, "addxp.invalid_amount");
            return;
        }

        if (amount < 0) {
            lang.send(sender, "addxp.negative_amount");
            return;
        }

        // Buscar al jugador
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            lang.send(sender, "addxp.player_not_found", "player", targetName);
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(targetPlayer.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(sender, "addxp.data_load_error");
                return;
            }

            // Agregar XP
            RPGPlayer updatedPlayer = rpgPlayer.get().addExperience(amount);
            playerManager.savePlayer(updatedPlayer);

            // Mensajes
            lang.send(sender, "addxp.success_admin", "amount", amount, "player", targetPlayer.getName());

            lang.send(targetPlayer, "addxp.notify_target", "amount", amount, "total", updatedPlayer.getExperience());

            lang.send(targetPlayer, "addxp.summary", "total", updatedPlayer.getExperience(), "level",
                    updatedPlayer.getLevel());

        } catch (Exception exception) {

            lang.send(sender, "addxp.error");
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
    public String getPermission() {
        return "rpgroll.admin.addxp";
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
