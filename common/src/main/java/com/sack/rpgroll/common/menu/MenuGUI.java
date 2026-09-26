package com.sack.rpgroll.common.menu;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;

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
 * Renderiza un {@link MenuDefinition} como GUI real, ejecutando las acciones
 * configuradas de cada {@link MenuItem} al hacer click.
 */
public class MenuGUI extends InventoryGUI {


    private final MenuDefinition menu;
    private final MenuActionExecutor actionExecutor;
    private final Map<Integer, List<MenuAction>> slotToActions = new HashMap<>();

    public MenuGUI(Player player, MenuDefinition menu, MenuActionExecutor actionExecutor) {
        super(player, ComponentUtils.parse(menu.title()), menu.rows() * 9);
        this.menu = menu;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void build() {

        clear();
        slotToActions.clear();

        fill();

        for (MenuItem menuItem : menu.items()) {

            if (menuItem.slot() < 0 || menuItem.slot() >= menu.rows() * 9) {
                continue;
            }

            if (menuItem.permission() != null && !player.hasPermission(menuItem.permission())) {
                continue;
            }

            Material material;

            try {
                material = Material.valueOf(menuItem.material().toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.BARRIER;
            }

            if (!material.isItem() || material.isAir()) {
                material = Material.BARRIER;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());

            // Una cabeza de jugador sin más muestra la de quien abre el menú.
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull && !skull.hasOwner()) {
                skull.setOwningPlayer(player);
            }

            if (!menuItem.displayName().isEmpty()) {
                meta.displayName(ComponentUtils.parse(menuItem.displayName().replace("{player}", player.getName())));
            }

            if (!menuItem.lore().isEmpty()) {
                List<Component> lore = menuItem.lore().stream()
                        .<Component>map(line -> ComponentUtils.parse(line.replace("{player}", player.getName())))
                        .toList();

                meta.lore(lore);
            }

            item.setItemMeta(meta);
            setItem(menuItem.slot(), item);

            slotToActions.put(menuItem.slot(), menuItem.actions());
        }
    }

    private void fill() {

        if (menu.filler() == null) {
            return;
        }

        Material material = Material.matchMaterial(menu.filler());
        if (material == null || !material.isItem()) {
            return;
        }

        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        filler.setItemMeta(meta);

        for (int slot = 0; slot < menu.rows() * 9; slot++) {
            setItem(slot, filler);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        List<MenuAction> actions = slotToActions.get(event.getRawSlot());

        if (actions == null) {
            return;
        }

        close();

        for (MenuAction action : actions) {
            actionExecutor.executeOne(player, action);
        }
    }

}