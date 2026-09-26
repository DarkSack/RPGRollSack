package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.menu.MenuActionExecutor;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Las acciones de los NPCs son las del motor de menús de RPGRoll-Lib; esto
 * solo añade ejecutar las de un NPC y buscar los menús en su carpeta menus/.
 */
public class NpcActionExecutor extends MenuActionExecutor {

    public NpcActionExecutor(Plugin plugin, NpcMenuManager menuManager) {
        super(plugin, menuManager::get);
    }

    public void execute(Player player, NpcDefinition npc) {
        executeAll(player, npc.actions());
    }

}
