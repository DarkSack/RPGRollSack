package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RodBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final FishingRodManager rodManager;
    private final ChatPromptManager chatPromptManager;
    private List<FishingRod> rods;

    public RodBrowserGUI(Player player, FishingRodManager rodManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Cañas de Pescar", NamedTextColor.GOLD), SIZE);
        this.rodManager = rodManager;
        this.chatPromptManager = chatPromptManager;
        this.rods = List.copyOf(rodManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < rods.size() && i < 36; i++) {

            FishingRod rod = rods.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(rod.material()))
                    .setName(Component.text(rod.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + rod.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear caña nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < rods.size() && slot < 36) {
            new RodEditorGUI(player, rods.get(slot), rodManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva caña:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (rodManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una caña con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            rodManager.save(new FishingRod(id, id, "FISHING_ROD", "", 64, 1.0, 1.0, 0, 1.0, 1.0, Set.of()));
            reopen();
        });
    }

    private void reopen() {
        this.rods = List.copyOf(rodManager.getAll());
        open();
    }

}
