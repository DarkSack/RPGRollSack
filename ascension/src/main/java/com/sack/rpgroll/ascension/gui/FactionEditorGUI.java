package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Faction;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FactionEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 11;
    private static final int BACK_SLOT = 26;

    private final FactionManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Faction current;

    public FactionEditorGUI(Player player, Faction faction, FactionManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Facción: " + faction.id(), NamedTextColor.GOLD), SIZE);
        this.current = faction;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Faction updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
                    value -> replace(new Faction(current.id(), value)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
