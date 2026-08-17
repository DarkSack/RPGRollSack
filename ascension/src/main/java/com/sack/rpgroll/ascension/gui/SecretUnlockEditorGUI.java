package com.sack.rpgroll.ascension.gui;

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

public class SecretUnlockEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int TYPE_SLOT = 10;
    private static final int TARGET_SLOT = 12;
    private static final int REQUIREMENTS_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final SecretUnlockManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private SecretUnlockRequirement current;

    public SecretUnlockEditorGUI(Player player, SecretUnlockRequirement unlock, SecretUnlockManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.secret_unlock.editor_title", "id", unlock.id()), SIZE);
        this.current = unlock;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(SecretUnlockRequirement updated) {
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

        setItem(TYPE_SLOT, new ItemBuilder(Material.ENDER_EYE)
                .setName(lang.component("gui.secret_unlock.type_label", "type", current.targetType()))
                .setLore(lang.component("gui.secret_unlock.type_hint"))
                .build());

        setItem(TARGET_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.secret_unlock.target_label", "id", current.targetId()))
                .setLore(lang.component("gui.secret_unlock.target_hint"))
                .build());

        setItem(REQUIREMENTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.secret_unlock.requirements_label"))
                .setLore(Component.text(RequirementsPrompt.format(current.requirements()), NamedTextColor.GRAY),
                        lang.component("gui.secret_unlock.requirements_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == TYPE_SLOT) {
            chatPromptManager.prompt(player, "gui.secret_unlock.prompt_type", value -> {
                try {
                    SecretTargetType type = SecretTargetType.valueOf(value.trim().toUpperCase());
                    replace(new SecretUnlockRequirement(current.id(), type, current.targetId(),
                            current.requirements()));
                } catch (IllegalArgumentException e) {
                    lang.send(player, "gui.common.invalid_type");
                }
            });
            return;
        }

        if (slot == TARGET_SLOT) {
            chatPromptManager.prompt(player, "gui.secret_unlock.prompt_target",
                    value -> replace(new SecretUnlockRequirement(current.id(), current.targetType(), value.trim(),
                            current.requirements())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player, "gui.secret_unlock.prompt_requirements",
                    value -> {
                        try {
                            replace(new SecretUnlockRequirement(current.id(), current.targetType(),
                                    current.targetId(), RequirementsPrompt.parse(value)));
                        } catch (NumberFormatException e) {
                            lang.send(player, "gui.common.invalid_format");
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
