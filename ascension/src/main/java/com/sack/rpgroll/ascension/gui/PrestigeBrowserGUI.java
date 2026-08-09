package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.PrestigeLevel;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class PrestigeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final PrestigeManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<PrestigeLevel> levels;

    public PrestigeBrowserGUI(Player player, PrestigeManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Prestigio RPGRoll-Ascension", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.levels = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < levels.size() && i < 36; i++) {
            PrestigeLevel level = levels.get(i);
            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(Component.text("Prestigio " + level.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Nivel requerido: " + level.requiredLevel(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear rango nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < levels.size() && slot < 36) {
            new PrestigeEditorGUI(player, levels.get(slot), manager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el número del nuevo rango de prestigio (ej. 3):", value -> {

            String id = value.trim();

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un rango con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            manager.save(new PrestigeLevel(id, 100, 0, List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.levels = List.copyOf(manager.getAll());
        open();
    }

}
