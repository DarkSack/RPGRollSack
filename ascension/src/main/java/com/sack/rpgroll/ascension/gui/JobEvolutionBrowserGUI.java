package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class JobEvolutionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final JobEvolutionManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<JobEvolution> evolutions;

    public JobEvolutionBrowserGUI(Player player, JobEvolutionManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Evoluciones de job", NamedTextColor.GOLD), SIZE);
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
            JobEvolution evolution = evolutions.get(i);
            setItem(i, new ItemBuilder(Material.IRON_PICKAXE)
                    .setName(Component.text(evolution.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Base: " + evolution.baseJob(), NamedTextColor.GRAY),
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
            new JobEvolutionEditorGUI(player, evolutions.get(slot), manager, chatPromptManager, this::reopen).open();
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
                player.sendMessage(Component.text("Ya existe una evolución de job con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "Escribí el id del job base:", baseJob -> {
                manager.save(new JobEvolution(id, baseJob.trim().toLowerCase(Locale.ROOT), id, 0, List.of(),
                        List.of(), List.of()));
                reopen();
            });
        });
    }

    private void reopen() {
        this.evolutions = List.copyOf(manager.getAll());
        open();
    }

}
