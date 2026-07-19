package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.levelup.PlayerLevelUpHandler;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para testear level up (solo admin, para debugging).
 * Uso: /rpg levelup
 */
public class LevelUpDebugCommand implements RPGCommand {

    private final RPGRoll plugin;

    public LevelUpDebugCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!sender.hasPermission("rpgroll.admin.*") && !sender.hasPermission("rpgroll.admin.reload")) {
            sender.sendMessage(Component.text("No tienes permisos para usar este comando.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) sender;

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            LevelUpRewardsConfig rewardsConfig = plugin.getBootstrap()
                    .getServices()
                    .get(LevelUpRewardsConfig.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                return;
            }

            PlayerLevelUpHandler handler = new PlayerLevelUpHandler(playerManager, rewardsConfig);

            if (handler.tryLevelUp(player, rpgPlayer.get())) {
                player.sendMessage(Component.text("✔ Level up procesado exitosamente.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(
                        Component.text("No tienes suficiente XP para subir de nivel.", NamedTextColor.YELLOW));
            }

        } catch (Exception exception) {

            player.sendMessage(Component.text("Error al procesar level up.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    @Override
    public String getName() {
        return "levelup";
    }

    @Override
    public String getDescription() {
        return "Test level up (admin only)";
    }

    @Override
    public String getUsage() {
        return "/rpg levelup";
    }

    @Override
    public List<String> getAliases() {
        return List.of("lvlup", "testlvl");
    }

}
