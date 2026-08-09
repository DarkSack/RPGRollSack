package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.Treasure;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TreasureBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TreasureManager treasureManager;
    private final ChatPromptManager chatPromptManager;
    private List<Treasure> treasures;

    public TreasureBrowserGUI(Player player, TreasureManager treasureManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Tesoros"), SIZE);
        this.treasureManager = treasureManager;
        this.chatPromptManager = chatPromptManager;
        this.treasures = List.copyOf(treasureManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < treasures.size() && i < 36; i++) {

            Treasure treasure = treasures.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(treasure.icon()))
                    .setName(Component.text(treasure.displayName(), NamedTextColor.GOLD))
                    .setLore(Component.text("id: " + treasure.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear tesoro nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < treasures.size() && slot < 36) {
            new TreasureEditorGUI(player, treasures.get(slot), treasureManager, chatPromptManager, this::reopen)
                    .open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo tesoro:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (treasureManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un tesoro con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            treasureManager.save(new Treasure(id, id, "CHEST", "", FishRarity.UNCOMMON, "CHEST", 1, 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.treasures = List.copyOf(treasureManager.getAll());
        open();
    }

}
