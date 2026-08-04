package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.FishingRegion;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FishingRegionEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int BOUNDS_SLOT = 10;
    private static final int CORNER_A_SLOT = 11;
    private static final int CORNER_B_SLOT = 12;
    private static final int WATER_TYPE_SLOT = 13;
    private static final int BACK_SLOT = 40;

    private final FishingRegionManager regionManager;
    private final Runnable onBack;
    private FishingRegion current;

    public FishingRegionEditorGUI(Player player, FishingRegion region, FishingRegionManager regionManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Región: " + region.id(), NamedTextColor.GOLD), SIZE);
        this.current = region;
        this.regionManager = regionManager;
        this.onBack = onBack;
    }

    private void replace(FishingRegion updated) {
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

        setItem(BOUNDS_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Mundo: " + current.world(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.format("(%.0f,%.0f,%.0f) a (%.0f,%.0f,%.0f)",
                        current.minX(), current.minY(), current.minZ(), current.maxX(), current.maxY(), current.maxZ()),
                        NamedTextColor.GRAY))
                .build());

        setItem(CORNER_A_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                .setName(Component.text("Fijar esquina A (tu posición)", NamedTextColor.YELLOW)).build());

        setItem(CORNER_B_SLOT, new ItemBuilder(Material.BLUE_CONCRETE)
                .setName(Component.text("Fijar esquina B (tu posición)", NamedTextColor.YELLOW)).build());

        setItem(WATER_TYPE_SLOT, new ItemBuilder(Material.WATER_BUCKET)
                .setName(Component.text("Agua forzada: " + current.forcedWaterType(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CORNER_A_SLOT) {
            Location loc = player.getLocation();
            replace(new FishingRegion(current.id(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(),
                    loc.getBlockZ(), current.maxX(), current.maxY(), current.maxZ(), current.forcedWaterType()));
            return;
        }

        if (slot == CORNER_B_SLOT) {
            Location loc = player.getLocation();
            replace(new FishingRegion(current.id(), loc.getWorld().getName(), current.minX(), current.minY(),
                    current.minZ(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), current.forcedWaterType()));
            return;
        }

        if (slot == WATER_TYPE_SLOT) {
            WaterType[] values = WaterType.values();
            WaterType next = values[(current.forcedWaterType().ordinal() + 1) % values.length];
            replace(new FishingRegion(current.id(), current.world(), current.minX(), current.minY(), current.minZ(),
                    current.maxX(), current.maxY(), current.maxZ(), next));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
