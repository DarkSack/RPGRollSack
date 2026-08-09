package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.LegacyTier;
import com.sack.rpgroll.ascension.deferred.LegacyManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class LegacyBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final LegacyManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<LegacyTier> tiers;

    public LegacyBrowserGUI(Player player, LegacyManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Legado RPGRoll-Ascension", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.tiers = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < tiers.size() && i < 36; i++) {
            LegacyTier tier = tiers.get(i);
            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(Component.text(tier.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Prestigio " + tier.requiredPrestige(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear tier nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < tiers.size() && slot < 36) {
            new LegacyEditorGUI(player, tiers.get(slot), manager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo tier (ej. 3):", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un tier con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            manager.save(new LegacyTier(id, 1, 5, 0));
            reopen();
        });
    }

    private void reopen() {
        this.tiers = List.copyOf(manager.getAll());
        open();
    }

}
