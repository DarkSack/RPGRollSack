package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.format.NamedTextColor;

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

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(NamedTextColor.RED + "Error al cargar tus datos.");
                return;
            }

            displayTraits(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(NamedTextColor.RED + "Error al cargar traits.");
            exception.printStackTrace();

        }

    }

    private void displayTraits(Player player, RPGPlayer rpgPlayer) {

        var traits = rpgPlayer.getTraits();

        player.sendMessage("");
        player.sendMessage(NamedTextColor.GOLD + "============== Tus Traits ==============");

        if (traits.getTraitIds().isEmpty()) {
            player.sendMessage(NamedTextColor.YELLOW + "Aún no tienes traits adquiridos.");
        } else {
            for (String traitId : traits.getTraitIds()) {
                player.sendMessage(NamedTextColor.LIGHT_PURPLE + "✦ " + NamedTextColor.WHITE + traitId);
            }
        }

        player.sendMessage(NamedTextColor.GRAY + "Cantidad total: " + NamedTextColor.WHITE + traits.count());
        player.sendMessage(NamedTextColor.GOLD + "========================================");
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

}
