package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AchievementEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 16;
    private static final int BACK_SLOT = 26;

    private final AchievementManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Achievement current;

    public AchievementEditorGUI(Player player, Achievement achievement, AchievementManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Logro: " + achievement.id(), NamedTextColor.GOLD), SIZE);
        this.current = achievement;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Achievement updated) {
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

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
                    value -> replace(new Achievement(current.id(), value, current.description())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:",
                    value -> replace(new Achievement(current.id(), current.displayName(), value)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
