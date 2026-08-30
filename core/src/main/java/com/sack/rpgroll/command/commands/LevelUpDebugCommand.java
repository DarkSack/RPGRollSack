package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.levelup.PlayerLevelUpHandler;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

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

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

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
                lang.send(player, "error.load_data");
                return;
            }

            PlayerLevelUpHandler handler = new PlayerLevelUpHandler(playerManager, rewardsConfig, lang);

            if (handler.tryLevelUp(player, rpgPlayer.get())) {
                lang.send(player, "levelup_debug_command.success");
            } else {
                lang.send(player, "levelup_debug_command.no_xp");
            }

        } catch (Exception exception) {

            lang.send(player, "levelup_debug_command.error");
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

    @Override
    public String getPermission() {
        return "rpgroll.admin.levelup";
    }

}
