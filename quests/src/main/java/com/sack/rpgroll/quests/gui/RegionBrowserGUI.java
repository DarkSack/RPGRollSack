package com.sack.rpgroll.quests.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.quests.region.Region;
import com.sack.rpgroll.quests.region.RegionManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class RegionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final RegionManager regionManager;
    private final ChatPromptManager chatPromptManager;
    private List<Region> regions;

    public RegionBrowserGUI(Player player, RegionManager regionManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Regiones RPGRoll", NamedTextColor.GOLD), SIZE);
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
        this.regions = List.copyOf(regionManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < regions.size() && i < 36; i++) {

            Region region = regions.get(i);

            setItem(i, new ItemBuilder(Material.MAP)
                    .setName(Component.text(region.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Mundo: " + region.world(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear región nueva (acá)", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < regions.size() && slot < 36) {
            new RegionEditorGUI(player, regions.get(slot), regionManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva región:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (regionManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una región con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Location loc = player.getLocation();
            Region region = new Region(id, loc.getWorld().getName(), loc.getX() - 5, loc.getY() - 5, loc.getZ() - 5,
                    loc.getX() + 5, loc.getY() + 5, loc.getZ() + 5);

            regionManager.save(region);
            reopen();
        });
    }

    private void reopen() {
        this.regions = List.copyOf(regionManager.getAll());
        open();
    }

}
