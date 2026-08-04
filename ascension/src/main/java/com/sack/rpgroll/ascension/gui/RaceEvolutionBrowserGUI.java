package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.core.RaceEvolutionManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RaceEvolutionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final RaceEvolutionManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<RaceEvolution> evolutions;

    public RaceEvolutionBrowserGUI(Player player, RaceEvolutionManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Evoluciones de raza", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.evolutions = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < evolutions.size() && i < 36; i++) {
            RaceEvolution evolution = evolutions.get(i);
            setItem(i, new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(Component.text(evolution.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Base: " + evolution.baseRace(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear evolución nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < evolutions.size() && slot < 36) {
            new RaceEvolutionEditorGUI(player, evolutions.get(slot), manager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva evolución:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una evolución con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "Escribí el id de la raza base:", baseRace -> {
                manager.save(new RaceEvolution(id, baseRace.trim().toLowerCase(Locale.ROOT), id,
                        AscensionRequirements.none(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(),
                        List.of()));
                reopen();
            });
        });
    }

    private void reopen() {
        this.evolutions = List.copyOf(manager.getAll());
        build();
    }

}
