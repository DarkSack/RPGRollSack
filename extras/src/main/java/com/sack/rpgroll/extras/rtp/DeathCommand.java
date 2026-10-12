package com.sack.rpgroll.extras.rtp;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /death}: lleva al punto de la última muerte, el que guarda el propio juego. A diferencia de
 * un {@code /back}, sigue llevando allí aunque después se haya usado otro teletransporte.
 */
public class DeathCommand implements CommandExecutor {

    private final LangManager lang;

    public DeathCommand(LangManager lang) {
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "server_menu.players_only");
            return true;
        }

        Location death = player.getLastDeathLocation();

        // El mundo puede no estar ya: una mazmorra que se cerró.
        // shortcut: no se mira cómo está el sitio (lava, o un mundo regenerado con el mismo nombre), igual
        // que el /back de Essentials; comprobarlo si empieza a matar gente.
        if (death == null || !death.isWorldLoaded()) {
            lang.send(player, "death.none");
            return true;
        }

        player.teleportAsync(death).thenAccept(arrived -> lang.send(player, arrived ? "death.done" : "death.none"));
        return true;
    }

}
