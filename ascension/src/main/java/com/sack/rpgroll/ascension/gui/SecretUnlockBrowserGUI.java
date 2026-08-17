package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.ascension.deferred.SecretTargetType;
import com.sack.rpgroll.ascension.deferred.SecretUnlockRequirement;
import com.sack.rpgroll.ascension.deferred.SecretUnlockManager;
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

public class SecretUnlockBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SecretUnlockManager manager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<SecretUnlockRequirement> unlocks;

    public SecretUnlockBrowserGUI(Player player, SecretUnlockManager manager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        super(player, lang.component("gui.secret_unlock.browser_title"), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.unlocks = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < unlocks.size() && i < 36; i++) {
            SecretUnlockRequirement unlock = unlocks.get(i);
            setItem(i, new ItemBuilder(Material.ENDER_EYE)
                    .setName(Component.text(unlock.id(), NamedTextColor.YELLOW))
                    .setLore(lang.component("gui.secret_unlock.item_target", "type", unlock.targetType(), "target",
                            unlock.targetId()), lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.secret_unlock.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < unlocks.size() && slot < 36) {
            new SecretUnlockEditorGUI(player, unlocks.get(slot), manager, chatPromptManager, this::reopen, lang)
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
        chatPromptManager.prompt(player, "gui.secret_unlock.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                lang.send(player, "gui.secret_unlock.id_exists");
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "gui.secret_unlock.prompt_target_new",
                    targetId -> {
                        manager.save(new SecretUnlockRequirement(id, SecretTargetType.TRAIT, targetId.trim(),
                                AscensionRequirements.none()));
                        reopen();
                    });
        });
    }

    private void reopen() {
        this.unlocks = List.copyOf(manager.getAll());
        open();
    }

}
