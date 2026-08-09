package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.fuel.FuelDefinition;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class FuelBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final FuelManager fuelManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<FuelDefinition> fuels;

    public FuelBrowserGUI(Player player, FuelManager fuelManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Combustibles", NamedTextColor.GOLD), SIZE);
        this.fuelManager = fuelManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.fuels = List.copyOf(fuelManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < fuels.size() && i < 36; i++) {

            FuelDefinition fuel = fuels.get(i);

            setItem(i, new ItemBuilder(parseMaterial(fuel.icon()))
                    .setName(Component.text(fuel.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + fuel.id(), NamedTextColor.GRAY),
                            Component.text("burn-ticks: " + fuel.burnTicks(), NamedTextColor.AQUA),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear combustible nuevo", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < fuels.size() && slot < 36) {
            new FuelEditorGUI(player, fuels.get(slot), fuelManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id del nuevo combustible:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (fuelManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un combustible con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            fuelManager.save(new FuelDefinition(id, id, "COAL", "COAL", false, 200, 1));
            reopen();
        });
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.COAL;
        }
    }

    private void reopen() {
        this.fuels = List.copyOf(fuelManager.getAll());
        open();
    }

}
