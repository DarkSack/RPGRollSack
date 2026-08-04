package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.BaitManager;
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

public class BaitBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final BaitManager baitManager;
    private final ChatPromptManager chatPromptManager;
    private List<Bait> baits;

    public BaitBrowserGUI(Player player, BaitManager baitManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Carnadas", NamedTextColor.GOLD), SIZE);
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
        this.baits = List.copyOf(baitManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < baits.size() && i < 36; i++) {

            Bait bait = baits.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(bait.material()))
                    .setName(Component.text(bait.displayName(), NamedTextColor.GREEN))
                    .setLore(Component.text("id: " + bait.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear carnada nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < baits.size() && slot < 36) {
            new BaitEditorGUI(player, baits.get(slot), baitManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva carnada:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (baitManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una carnada con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            baitManager.save(new Bait(id, id, "STRING", "", Set.of(), 0, 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.baits = List.copyOf(baitManager.getAll());
        build();
    }

}
