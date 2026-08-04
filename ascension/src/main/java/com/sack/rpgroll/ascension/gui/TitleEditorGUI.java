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

public class TitleEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 11;
    private static final int BACK_SLOT = 26;

    private final TitleManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Title current;

    public TitleEditorGUI(Player player, Title title, TitleManager manager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Título: " + title.id(), NamedTextColor.GOLD), SIZE);
        this.current = title;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Title updated) {
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
                    value -> replace(new Title(current.id(), value)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
