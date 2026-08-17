package com.sack.rpgroll.quests.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.quests.region.Region;
import com.sack.rpgroll.quests.region.RegionManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Editor de una región — "Fijar mínimo/máximo acá" usa la ubicación actual del jugador, sin herramienta de selección. */
public class RegionEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int WORLD_SLOT = 10;
    private static final int MIN_SLOT = 11;
    private static final int MAX_SLOT = 12;
    private static final int INFO_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final RegionManager regionManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Region current;

    public RegionEditorGUI(Player player, Region region, RegionManager regionManager, Runnable onBack,
            LangManager lang) {
        super(player, lang.component("region_editor.title", "id", region.id()), SIZE);
        this.current = region;
        this.regionManager = regionManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(Region updated) {
        current = updated;
        regionManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(WORLD_SLOT, new ItemBuilder(Material.GRASS_BLOCK)
                .setName(lang.component("region_editor.world_label", "world", current.world()))
                .setLore(lang.component("region_editor.click_use_current_world"))
                .build());

        setItem(MIN_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                .setName(lang.component("region_editor.set_min_corner"))
                .setLore(lang.component("region_editor.coords", "x", (int) current.minX(), "y", (int) current.minY(),
                        "z", (int) current.minZ()))
                .build());

        setItem(MAX_SLOT, new ItemBuilder(Material.LIME_CONCRETE)
                .setName(lang.component("region_editor.set_max_corner"))
                .setLore(lang.component("region_editor.coords", "x", (int) current.maxX(), "y", (int) current.maxY(),
                        "z", (int) current.maxZ()))
                .build());

        double volume = (current.maxX() - current.minX()) * (current.maxY() - current.minY())
                * (current.maxZ() - current.minZ());

        setItem(INFO_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("region_editor.volume_label", "volume", (long) volume))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("region_editor.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == WORLD_SLOT) {
            replace(new Region(current.id(), player.getWorld().getName(), current.minX(), current.minY(),
                    current.minZ(), current.maxX(), current.maxY(), current.maxZ()));
            return;
        }

        if (slot == MIN_SLOT) {
            Location loc = player.getLocation();
            replace(new Region(current.id(), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                    current.maxX(), current.maxY(), current.maxZ()));
            return;
        }

        if (slot == MAX_SLOT) {
            Location loc = player.getLocation();
            replace(new Region(current.id(), loc.getWorld().getName(), current.minX(), current.minY(),
                    current.minZ(), loc.getX(), loc.getY(), loc.getZ()));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
