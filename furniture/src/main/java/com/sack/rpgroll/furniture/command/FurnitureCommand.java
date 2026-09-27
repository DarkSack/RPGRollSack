package com.sack.rpgroll.furniture.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.core.CarpenterRecipe;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.gui.FurnitureMenu;
import com.sack.rpgroll.furniture.item.FurnitureItems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /muebles: el catálogo. {@code /muebles fabricar} abre el carpintero donde uno esté, solo con
 * el permiso {@code rpgroll.furniture.carpenter.anywhere}.
 */
public class FurnitureCommand implements CommandExecutor, TabCompleter {

    private final FurnitureManager manager;
    private final FurnitureItems items;
    private final LangManager lang;

    public FurnitureCommand(FurnitureManager manager, FurnitureItems items, LangManager lang) {
        this.manager = manager;
        this.items = items;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.players_only");
            return true;
        }

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("fabricar") || sub.equals("craft")) {
            if (!player.hasPermission("rpgroll.furniture.carpenter.anywhere")) {
                lang.send(player, "command.no_permission");
                return true;
            }
            String station = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : CarpenterRecipe.DEFAULT_STATION;
            new FurnitureMenu(player, manager, items, lang, FurnitureMenu.Mode.CARPENTER, station).open();
            return true;
        }

        new FurnitureMenu(player, manager, items, lang, FurnitureMenu.Mode.CATALOG, null).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1 && sender.hasPermission("rpgroll.furniture.carpenter.anywhere")) {
            return List.of("fabricar").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
