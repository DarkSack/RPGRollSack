package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.fuel.FuelDefinition;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FuelEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int MATERIAL_OR_ITEM_SLOT = 12;
    private static final int IS_CUSTOM_ITEM_SLOT = 13;
    private static final int BURN_TICKS_SLOT = 19;
    private static final int CONSUME_AMOUNT_SLOT = 20;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final FuelManager fuelManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private FuelDefinition current;

    public FuelEditorGUI(Player player, FuelDefinition fuel, FuelManager fuelManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Combustible: " + fuel.id(), NamedTextColor.GOLD), SIZE);
        this.current = fuel;
        this.fuelManager = fuelManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(FuelDefinition updated) {
        current = updated;
        fuelManager.save(current);
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

        setItem(ICON_SLOT, new ItemBuilder(FuelBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(MATERIAL_OR_ITEM_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Material/item-id: " + current.materialOrItemId(), NamedTextColor.AQUA))
                .build());

        setItem(IS_CUSTOM_ITEM_SLOT, new ItemBuilder(current.isCustomItem() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(Component.text("Es ítem personalizado: " + current.isCustomItem(), NamedTextColor.GOLD))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY)).build());

        setItem(BURN_TICKS_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Ticks de quemado: " + current.burnTicks(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +20 · Click derecho: -20", NamedTextColor.GRAY)).build());

        setItem(CONSUME_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Cantidad consumida: " + current.consumeAmount(), NamedTextColor.AQUA))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Eliminar combustible", NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new FuelDefinition(
                    current.id(), value, current.icon(), current.materialOrItemId(), current.isCustomItem(),
                    current.burnTicks(), current.consumeAmount())));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new FuelDefinition(
                    current.id(), current.displayName(), value, current.materialOrItemId(), current.isCustomItem(),
                    current.burnTicks(), current.consumeAmount())));
        } else if (slot == MATERIAL_OR_ITEM_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material vanilla o el id de ítem personalizado:",
                    value -> replace(new FuelDefinition(current.id(), current.displayName(), current.icon(), value,
                            current.isCustomItem(), current.burnTicks(), current.consumeAmount())));
        } else if (slot == IS_CUSTOM_ITEM_SLOT) {
            replace(new FuelDefinition(current.id(), current.displayName(), current.icon(),
                    current.materialOrItemId(), !current.isCustomItem(), current.burnTicks(), current.consumeAmount()));
        } else if (slot == BURN_TICKS_SLOT) {
            replace(new FuelDefinition(current.id(), current.displayName(), current.icon(),
                    current.materialOrItemId(), current.isCustomItem(), Math.max(1, current.burnTicks() + sign * 20),
                    current.consumeAmount()));
        } else if (slot == CONSUME_AMOUNT_SLOT) {
            replace(new FuelDefinition(current.id(), current.displayName(), current.icon(),
                    current.materialOrItemId(), current.isCustomItem(), current.burnTicks(),
                    Math.max(1, current.consumeAmount() + sign)));
        } else if (slot == DELETE_SLOT) {
            fuelManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
