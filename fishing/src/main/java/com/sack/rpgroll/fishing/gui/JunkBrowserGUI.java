package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class JunkBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final JunkManager junkManager;
    private final ChatPromptManager chatPromptManager;
    private List<Junk> junks;

    public JunkBrowserGUI(Player player, JunkManager junkManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Basura"), SIZE);
        this.junkManager = junkManager;
        this.chatPromptManager = chatPromptManager;
        this.junks = List.copyOf(junkManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < junks.size() && i < 36; i++) {

            Junk junk = junks.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(junk.icon()))
                    .setName(Component.text(junk.displayName(), NamedTextColor.GRAY))
                    .setLore(Component.text("id: " + junk.id(), NamedTextColor.DARK_GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear basura nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < junks.size() && slot < 36) {
            new JunkEditorGUI(player, junks.get(slot), junkManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva basura:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (junkManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe basura con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            junkManager.save(new Junk(id, id, "LEATHER_BOOTS", "", 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.junks = List.copyOf(junkManager.getAll());
        open();
    }

}
