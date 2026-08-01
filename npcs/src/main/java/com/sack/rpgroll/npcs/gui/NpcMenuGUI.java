package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.npcs.core.NpcAction;
import com.sack.rpgroll.npcs.core.NpcActionExecutor;
import com.sack.rpgroll.npcs.core.NpcMenuDefinition;
import com.sack.rpgroll.npcs.core.NpcMenuItem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderiza un NpcMenuDefinition como GUI real, ejecutando las acciones
 * configuradas de cada NpcMenuItem al hacer click.
 */
public class NpcMenuGUI extends InventoryGUI {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NpcMenuDefinition menu;
    private final NpcActionExecutor actionExecutor;
    private final Map<Integer, List<NpcAction>> slotToActions = new HashMap<>();

    public NpcMenuGUI(Player player, NpcMenuDefinition menu, NpcActionExecutor actionExecutor) {
        super(player, LEGACY.deserialize(menu.title()), menu.rows() * 9);
        this.menu = menu;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void build() {

        clear();
        slotToActions.clear();

        for (NpcMenuItem menuItem : menu.items()) {

            if (menuItem.slot() < 0 || menuItem.slot() >= menu.rows() * 9) {
                continue;
            }

            Material material;

            try {
                material = Material.valueOf(menuItem.material().toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.BARRIER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (!menuItem.displayName().isEmpty()) {
                meta.displayName(LEGACY.deserialize(menuItem.displayName()));
            }

            if (!menuItem.lore().isEmpty()) {
                List<Component> lore = menuItem.lore().stream()
                        .<Component>map(LEGACY::deserialize)
                        .toList();

                meta.lore(lore);
            }

            item.setItemMeta(meta);
            setItem(menuItem.slot(), item);

            slotToActions.put(menuItem.slot(), menuItem.actions());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        List<NpcAction> actions = slotToActions.get(event.getRawSlot());

        if (actions == null || actions.isEmpty()) {
            return;
        }

        close();

        for (NpcAction action : actions) {
            actionExecutor.executeOne(player, action);
        }
    }

}