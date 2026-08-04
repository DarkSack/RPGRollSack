package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcMenuDefinition;
import com.sack.rpgroll.npcs.core.NpcMenuManager;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class NpcMenuBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final NpcMenuManager menuManager;
    private final ChatPromptManager chatPromptManager;
    private List<NpcMenuDefinition> menus;

    public NpcMenuBrowserGUI(Player player, NpcMenuManager menuManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Menús de NPC RPGRoll", NamedTextColor.GOLD), SIZE);
        this.menuManager = menuManager;
        this.chatPromptManager = chatPromptManager;
        this.menus = List.copyOf(menuManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < menus.size() && i < 36; i++) {

            NpcMenuDefinition menu = menus.get(i);

            setItem(i, new ItemBuilder(Material.CHEST)
                    .setName(Component.text(menu.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(menu.items().size() + " ítem(s) · " + menu.rows() + " fila(s)",
                            NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear menú nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < menus.size() && slot < 36) {
            new NpcMenuEditorGUI(player, menus.get(slot), menuManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo menú:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (menuManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un menú con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            NpcMenuDefinition menu = new NpcMenuDefinition(id, "&8" + id, 3, List.of());
            menuManager.save(menu);
            reopen();
        });
    }

    private void reopen() {
        this.menus = List.copyOf(menuManager.getAll());
        build();
    }

}
