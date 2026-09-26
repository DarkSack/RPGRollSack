package com.sack.rpgroll.extras.menu;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /menu} abre el menú del servidor; {@code /menu <id>} cualquier menú
 * de menus/, solo con {@code rpgrollextras.menu.any} (un menú puede llevar
 * comandos de consola). {@code /menu item} devuelve la brújula.
 */
public class MenuCommand implements CommandExecutor, TabCompleter {

    public static final String ANY_PERMISSION = "rpgrollextras.menu.any";

    private final ServerMenu menu;
    private final ExtrasMenuManager menus;
    private final LangManager lang;

    public MenuCommand(ServerMenu menu, ExtrasMenuManager menus, LangManager lang) {
        this.menu = menu;
        this.menus = menus;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "server_menu.players_only");
            return true;
        }

        if (args.length == 0) {
            menu.openMain(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("item")) {
            if (!menu.config().enabled()) {
                lang.send(player, "server_menu.disabled");
                return true;
            }
            menu.give(player);
            lang.send(player, "server_menu.item_given");
            return true;
        }

        if (!player.hasPermission(ANY_PERMISSION)) {
            lang.send(player, "server_menu.no_permission");
            return true;
        }

        if (!menu.open(player, args[0])) {
            lang.send(player, "server_menu.unknown", "id", args[0]);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length != 1) {
            return List.of();
        }

        List<String> options = new java.util.ArrayList<>(List.of("item"));
        if (sender.hasPermission(ANY_PERMISSION)) {
            menus.getAll().forEach(definition -> options.add(definition.id()));
        }

        return TabCompleteUtil.filter(args[0], options);
    }

}
