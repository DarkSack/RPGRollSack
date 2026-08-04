package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.SecretTargetType;
import com.sack.rpgroll.ascension.deferred.SecretUnlockRequirement;
import com.sack.rpgroll.ascension.deferred.SecretUnlockManager;
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
    private SecretUnlockRequirement current;

    public SecretUnlockEditorGUI(Player player, SecretUnlockRequirement unlock, SecretUnlockManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Secreto: " + unlock.id(), NamedTextColor.GOLD), SIZE);
        this.current = unlock;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                .setName(Component.text("Tipo: " + current.targetType(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para cambiar (RACE/CLASS/TRAIT)", NamedTextColor.GRAY))
                .build());

        setItem(TARGET_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Id objetivo: " + current.targetId(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir el id real en :core", NamedTextColor.GRAY))
                .build());

        setItem(REQUIREMENTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Requisitos", NamedTextColor.YELLOW))
                .setLore(Component.text(RequirementsPrompt.format(current.requirements()), NamedTextColor.GRAY),
                        Component.text("Click para escribir: nivel;prestigio;trait;quests;facción=rep",
                                NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == TYPE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el tipo (RACE, CLASS o TRAIT):", value -> {
                try {
                    SecretTargetType type = SecretTargetType.valueOf(value.trim().toUpperCase());
                    replace(new SecretUnlockRequirement(current.id(), type, current.targetId(),
                            current.requirements()));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("Tipo inválido.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == TARGET_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id real del contenido en :core:",
                    value -> replace(new SecretUnlockRequirement(current.id(), current.targetType(), value.trim(),
                            current.requirements())));
            return;
        }

        if (slot == REQUIREMENTS_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: nivel;prestigio;trait;quest1,quest2;facción=rep,facción2=rep (ej. 20;0;-;;reino=50):",
                    value -> {
                        try {
                            replace(new SecretUnlockRequirement(current.id(), current.targetType(),
                                    current.targetId(), RequirementsPrompt.parse(value)));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
