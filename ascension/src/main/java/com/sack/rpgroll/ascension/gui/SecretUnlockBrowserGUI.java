package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
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

import java.util.List;
import java.util.Locale;

public class SecretUnlockBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SecretUnlockManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<SecretUnlockRequirement> unlocks;

    public SecretUnlockBrowserGUI(Player player, SecretUnlockManager manager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Desbloqueos secretos", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
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
                    .setLore(Component.text(unlock.targetType() + ": " + unlock.targetId(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear desbloqueo nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < unlocks.size() && slot < 36) {
            new SecretUnlockEditorGUI(player, unlocks.get(slot), manager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo desbloqueo secreto:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un desbloqueo con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "Escribí el id real del contenido en :core (raza/clase/trait):",
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
