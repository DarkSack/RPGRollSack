package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.gui.HelpMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** {@code /structures [help|open]}. help y open abren la ayuda; list, info, status y cancel llegan en el plan 3. */
public final class StructuresCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("help", "open");

    private final StructuresPlugin plugin;

    public StructuresCommand(StructuresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = Senders.asPlayer(sender);
        if (player == null) {
            plugin.lang().send(sender, "command.player-only");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (SUBS.contains(sub)) {
            new HelpMenu(player, plugin, 0).open();
        } else {
            plugin.lang().send(sender, "command.usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? SUBS : List.of();
    }
}
