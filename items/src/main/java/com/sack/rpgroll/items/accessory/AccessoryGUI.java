package com.sack.rpgroll.items.accessory;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Las ranuras de amuleto. Se mueve con un clic en cada sentido en vez de
 * arrastrar: así no hay cursor ni arrastre que duplique o pierda un ítem.
 */
public class AccessoryGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int[] SLOT_POSITIONS = { 11, 13, 15 };

    private final AccessoryService service;
    private final LangManager lang;

    public AccessoryGUI(Player player, AccessoryService service, LangManager lang) {
        super(player, lang.component("accessory.title"), SIZE);
        this.service = service;
        this.lang = lang;
    }

    @Override
    public void build() {

        for (int i = 0; i < SIZE; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        ItemStack[] worn = service.worn(player);

        for (int i = 0; i < AccessoryService.SLOTS; i++) {
            setItem(SLOT_POSITIONS[i], worn[i] != null ? worn[i]
                    : new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                            .setName(lang.component("accessory.empty_slot"))
                            .setLore(lang.component("accessory.empty_slot_lore"))
                            .build());
        }
    }

    /** Clic en una ranura ocupada: el amuleto vuelve al inventario. */
    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        for (int i = 0; i < AccessoryService.SLOTS; i++) {
            if (event.getRawSlot() != SLOT_POSITIONS[i] || service.worn(player)[i] == null) {
                continue;
            }
            if (player.getInventory().firstEmpty() < 0) {
                lang.send(player, "accessory.inventory_full");
                return;
            }
            ItemStack removed = service.unequip(player, i);
            player.getInventory().addItem(removed);
            lang.send(player, "accessory.removed", "item", name(removed));
            build();
            return;
        }
    }

    /** Clic en un amuleto del inventario: una unidad pasa a la primera ranura libre. */
    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        switch (service.equip(player, clicked)) {
            case OK -> {
                lang.send(player, "accessory.equipped", "item", name(clicked));
                clicked.setAmount(clicked.getAmount() - 1);
                build();
            }
            case NOT_ACCESSORY -> lang.send(player, "accessory.not_accessory");
            case DUPLICATE -> lang.send(player, "accessory.duplicate");
            case FULL -> lang.send(player, "accessory.full");
        }
    }

    private static Component name(ItemStack item) {
        return item.effectiveName();
    }

}
