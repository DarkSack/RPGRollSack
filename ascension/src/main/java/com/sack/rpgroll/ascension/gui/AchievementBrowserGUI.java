package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private List<Achievement> achievements;

    public AchievementBrowserGUI(Player player, AchievementManager manager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        super(player, lang.component("gui.achievement.browser_title"), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
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
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.achievement.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < achievements.size() && slot < 36) {
            new AchievementEditorGUI(player, achievements.get(slot), manager, chatPromptManager, this::reopen, lang)
                    .open();
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
        chatPromptManager.prompt(player, "gui.achievement.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                lang.send(player, "gui.achievement.id_exists");
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
