package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver los traits del jugador.
 * Uso: /rpg traits
 */
public class TraitsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public TraitsCommand(RPGRoll plugin) {
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
                lang.send(player, "traits_command.not_found");
                lang.send(player, "error.load_data");
                return;
            }

            displayTraits(lang, player, rpgPlayer.get());

        } catch (Exception exception) {

            lang.send(player, "traits_command.error");
            exception.printStackTrace();

        }

    }

    private void displayTraits(LangManager lang, Player player, RPGPlayer rpgPlayer) {

        var traits = rpgPlayer.getTraits();

        player.sendMessage("");
        lang.send(player, "traits_command.header");

        if (traits.getTraitIds().isEmpty()) {
            lang.send(player, "traits_command.none");
        } else {
            for (String traitId : traits.getTraitIds()) {
                lang.send(player, "traits_command.entry", "trait", traitId);
            }
        }

        lang.send(player, "traits_command.count", "count", traits.count());
        lang.send(player, "traits_command.footer");
        player.sendMessage("");

    }

    @Override
    public String getName() {
        return "traits";
    }

    @Override
    public String getDescription() {
        return "Muestra tus traits adquiridos";
    }

    @Override
    public String getUsage() {
        return "/rpg traits";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rasgos", "trt");
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.traits";
    }

}
