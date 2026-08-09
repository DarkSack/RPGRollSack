package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Edita los campos escalares de una {@code CustomStation}. Los
 * {@code ingredient-slots} y {@code allowed-recipe-ids} son listas — se
 * editan directamente en el YAML de la estación (mismo criterio que el resto
 * del ecosistema para campos de lista/mapa anidados).
 */
public class CustomStationEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int BLOCK_SLOT = 12;
    private static final int INVENTORY_SIZE_SLOT = 13;
    private static final int FUEL_SLOT_SLOT = 19;
    private static final int OUTPUT_SLOT_SLOT = 20;
    private static final int REQUIRES_FUEL_SLOT = 21;
    private static final int GUI_TITLE_SLOT = 22;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final CustomStationManager stationManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private CustomStation current;

    public CustomStationEditorGUI(Player player, CustomStation station, CustomStationManager stationManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Estación: " + station.id(), NamedTextColor.GOLD), SIZE);
        this.current = station;
        this.stationManager = stationManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(CustomStation updated) {
        current = updated;
        stationManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(CustomStationBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(BLOCK_SLOT, new ItemBuilder(CustomStationBrowserGUI.parseMaterial(current.triggerBlockMaterial()))
                .setName(Component.text("Bloque disparador: " + current.triggerBlockMaterial(), NamedTextColor.AQUA))
                .build());

        setItem(INVENTORY_SIZE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Tamaño de inventario: " + current.inventorySize(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +9 · Click derecho: -9", NamedTextColor.GRAY)).build());

        setItem(FUEL_SLOT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Slot de combustible: " + current.fuelSlot(), NamedTextColor.GOLD))
                .setLore(Component.text("Click: +1 · Click derecho: -1 (-1 = sin combustible)", NamedTextColor.GRAY))
                .build());

        setItem(OUTPUT_SLOT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Slot de salida: " + current.outputSlot(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(REQUIRES_FUEL_SLOT, new ItemBuilder(current.requiresFuel() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(Component.text("Requiere combustible: " + current.requiresFuel(), NamedTextColor.GOLD))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY)).build());

        setItem(GUI_TITLE_SLOT, new ItemBuilder(Material.OAK_SIGN)
                .setName(Component.text("Título de GUI: " + current.guiTitle(), NamedTextColor.YELLOW)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Eliminar estación", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(withIcon(value)));
        } else if (slot == BLOCK_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del bloque disparador:",
                    value -> replace(withBlock(value)));
        } else if (slot == INVENTORY_SIZE_SLOT) {
            replace(withInventorySize(current.inventorySize() + sign * 9));
        } else if (slot == FUEL_SLOT_SLOT) {
            replace(withFuelSlot(Math.max(-1, current.fuelSlot() + sign)));
        } else if (slot == OUTPUT_SLOT_SLOT) {
            replace(withOutputSlot(Math.max(0, current.outputSlot() + sign)));
        } else if (slot == REQUIRES_FUEL_SLOT) {
            replace(withRequiresFuel(!current.requiresFuel()));
        } else if (slot == GUI_TITLE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo título de GUI:", value -> replace(withGuiTitle(value)));
        } else if (slot == DELETE_SLOT) {
            stationManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private CustomStation withName(String v) {
        return new CustomStation(current.id(), v, current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), current.fuelSlot(), current.outputSlot(),
                current.requiresFuel(), current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withIcon(String v) {
        return new CustomStation(current.id(), current.displayName(), v, current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), current.fuelSlot(), current.outputSlot(),
                current.requiresFuel(), current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withBlock(String v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), v, current.inventorySize(),
                current.ingredientSlots(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withInventorySize(int v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                v, current.ingredientSlots(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withFuelSlot(int v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), v, current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withOutputSlot(int v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), current.fuelSlot(), v, current.requiresFuel(),
                current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withRequiresFuel(boolean v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), current.fuelSlot(), current.outputSlot(), v,
                current.guiTitle(), current.allowedRecipeIds());
    }

    private CustomStation withGuiTitle(String v) {
        return new CustomStation(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.ingredientSlots(), current.fuelSlot(), current.outputSlot(),
                current.requiresFuel(), v, current.allowedRecipeIds());
    }

}
