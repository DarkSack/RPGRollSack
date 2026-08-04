package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TitleBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TitleManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<Title> titles;

    public TitleBrowserGUI(Player player, TitleManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Títulos RPGRoll-Ascension", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.titles = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < titles.size() && i < 36; i++) {
            Title title = titles.get(i);
            setItem(i, new ItemBuilder(Material.NAME_TAG)
                    .setName(Component.text(title.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(title.displayName(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear título nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < titles.size() && slot < 36) {
            new TitleEditorGUI(player, titles.get(slot), manager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo título:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un título con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            manager.save(new Title(id, id));
            reopen();
        });
    }

    private void reopen() {
        this.titles = List.copyOf(manager.getAll());
        build();
    }

}
