package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Confirmación de 3 filas. La Lib no trae una (vive en core, del que este módulo no depende). */
public final class ConfirmMenu extends InventoryGUI {
    public static final int CONFIRM = 11;
    public static final int INFO = 13;
    public static final int CANCEL = 15;

    private final StructuresPlugin plugin;
    private final ItemStack info;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(Player player, StructuresPlugin plugin, WorkbenchTier tier, ItemStack info,
                       Runnable onConfirm, Runnable onCancel) {
        super(player, StructuresMenus.skin().title(StructuresMenus.background(StructuresMenus.CONFIRMAR, tier),
                plugin.menuTitle("confirm")), 27);
        this.plugin = plugin;
        this.info = info;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void build() {
        StructuresMenus.fill(inventory);
        put(INFO, info);
        put(CONFIRM, StructuresMenus.item(Material.LIME_CONCRETE, plugin.lang().component("menu.confirm.accept"),
                List.of(plugin.lang().component("menu.confirm.accept-lore"))));
        put(CANCEL, StructuresMenus.item(Material.RED_CONCRETE, plugin.lang().component("menu.confirm.cancel"),
                List.of(plugin.lang().component("menu.confirm.cancel-lore"))));
    }

    private void put(int slot, ItemStack item) {
        setItem(slot, StructuresMenus.CONFIRMAR.dress(slot, item));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == CONFIRM) {
            onConfirm.run();
        } else if (event.getRawSlot() == CANCEL) {
            onCancel.run();
        }
    }
}
