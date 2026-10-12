package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.extras.travel.Warps.Warp;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * /warps: los warps en un menú, para viajar con un clic en vez de escribir el nombre. Pensado para
 * Bedrock, donde escribir un comando cuesta; por eso también crea uno con el nombre del jugador.
 */
class WarpsGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int CREATE_SLOT = 49;

    private final WarpCommand command;
    private final LangManager lang;
    private final List<Warp> warps = new ArrayList<>();

    WarpsGUI(Player player, Iterable<Warp> all, WarpCommand command, LangManager lang) {

        super(player, lang.component("warp.menu_title"), SIZE);

        this.command = command;
        this.lang = lang;

        // shortcut: los 45 primeros; con más warps que eso hará falta paginar.
        for (Warp warp : all) {
            if (warps.size() < SIZE - 9) {
                warps.add(warp);
            }
        }
    }

    @Override
    public void build() {

        clear();

        for (int i = 0; i < warps.size(); i++) {

            Warp warp = warps.get(i);
            boolean mine = warp.owner().equals(player.getUniqueId());

            setItem(i, new ItemBuilder(mine ? Material.ENDER_EYE : Material.ENDER_PEARL)
                    .setName(lang.component("warp.menu_entry", "warp", warp.name()))
                    .setLore(lang.component("warp.menu_owner", "owner", warp.ownerName()),
                            lang.component("warp.menu_click"))
                    .build());
        }

        setItem(CREATE_SLOT, new ItemBuilder(Material.OAK_SIGN)
                .setName(lang.component("warp.menu_create"))
                .setLore(lang.component("warp.menu_create_lore"))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot >= 0 && slot < warps.size()) {
            close();
            command.travel(player, warps.get(slot).name());
        } else if (slot == CREATE_SLOT) {
            close();
            command.createHere(player);
        }
    }

}
