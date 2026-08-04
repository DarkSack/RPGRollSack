package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.RuneModifierType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RuneBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final RuneManager runeManager;
    private final ChatPromptManager chatPromptManager;
    private List<Rune> runes;

    public RuneBrowserGUI(Player player, RuneManager runeManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Runas"), SIZE);
        this.runeManager = runeManager;
        this.chatPromptManager = chatPromptManager;
        this.runes = List.copyOf(runeManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < runes.size() && i < 36; i++) {

            Rune rune = runes.get(i);

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(rune.icon()))
                    .setName(Component.text(rune.displayName(), NamedTextColor.GREEN))
                    .setLore(Component.text("id: " + rune.id(), NamedTextColor.GRAY),
                            Component.text("Tipo: " + rune.type(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear runa nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < runes.size() && slot < 36) {
            new RuneEditorGUI(player, runes.get(slot), runeManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva runa:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (runeManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una runa con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Rune rune = new Rune(id, id, "EMERALD", "", RuneModifierType.PIERCING, Map.of("max-pierces", "3"));
            runeManager.save(rune);
            reopen();
        });
    }

    private void reopen() {
        this.runes = List.copyOf(runeManager.getAll());
        build();
    }

}
