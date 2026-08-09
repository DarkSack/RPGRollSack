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

import java.util.List;
import java.util.Locale;

public class AchievementBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final AchievementManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<Achievement> achievements;

    public AchievementBrowserGUI(Player player, AchievementManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Logros RPGRoll-Ascension", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.achievements = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < achievements.size() && i < 36; i++) {
            Achievement achievement = achievements.get(i);
            setItem(i, new ItemBuilder(Material.GOLDEN_APPLE)
                    .setName(Component.text(achievement.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(achievement.displayName(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear logro nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < achievements.size() && slot < 36) {
            new AchievementEditorGUI(player, achievements.get(slot), manager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo logro:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un logro con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            manager.save(new Achievement(id, id, ""));
            reopen();
        });
    }

    private void reopen() {
        this.achievements = List.copyOf(manager.getAll());
        open();
    }

}
