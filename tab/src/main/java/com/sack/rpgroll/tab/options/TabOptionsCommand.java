package com.sack.rpgroll.tab.options;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.tab.tablist.TabListEngine;
import com.sack.rpgroll.tab.tablist.TablistDefinition;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /tab}: abre el menú para mostrar u ocultar los bloques de tu propia cabecera y pie. */
public class TabOptionsCommand implements CommandExecutor {

    private final TabListEngine engine;
    private final TabOptionsStore store;
    private final LangManager lang;

    public TabOptionsCommand(TabListEngine engine, TabOptionsStore store, LangManager lang) {
        this.engine = engine;
        this.store = store;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            lang.send(sender, "options.players_only");
            return true;
        }
        TablistDefinition tablist = engine.activeTablist(player);
        if (tablist == null || tablist.blocks().isEmpty()) {
            lang.send(player, "options.none");
            return true;
        }
        new TabOptionsMenu(player, tablist.blocks(), store, lang, engine::apply).open();
        return true;
    }
}
