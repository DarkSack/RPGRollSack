package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;

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
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(player, "stats_command.not_found");
                lang.send(player, "error.load_data");
                return;
            }

            displayStats(lang, player, rpgPlayer.get());

        } catch (Exception exception) {

            lang.send(player, "stats_command.error");
            exception.printStackTrace();

        }

    }

    private void displayStats(LangManager lang, Player player, RPGPlayer rpgPlayer) {

        var stats = rpgPlayer.getStats();

        lang.send(player, "stats.view_stats", "player", player.getName());
        lang.send(player, "stats.strength", "value", stats.strength());
        lang.send(player, "stats.dexterity", "value", stats.dexterity());
        lang.send(player, "stats.constitution", "value", stats.constitution());
        lang.send(player, "stats.intelligence", "value", stats.intelligence());
        lang.send(player, "stats.wisdom", "value", stats.wisdom());
        lang.send(player, "stats.charisma", "value", stats.charisma());
        lang.send(player, "stats.footer");

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

    @Override
    public String getPermission() {
        return "rpgroll.player.stats";
    }

}
