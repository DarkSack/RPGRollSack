package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.ClassSpecializationManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassSpecializationBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ClassSpecializationManager manager;
    private final ChatPromptManager chatPromptManager;
    private List<ClassSpecialization> specializations;

    public ClassSpecializationBrowserGUI(Player player, ClassSpecializationManager manager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Especializaciones de clase", NamedTextColor.GOLD), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.specializations = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < specializations.size() && i < 36; i++) {
            ClassSpecialization specialization = specializations.get(i);
            setItem(i, new ItemBuilder(Material.DIAMOND_SWORD)
                    .setName(Component.text(specialization.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Base: " + specialization.baseClass(), NamedTextColor.GRAY),
                            Component.text(specialization.talentTree().size() + " talento(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear especialización nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < specializations.size() && slot < 36) {
            new ClassSpecializationEditorGUI(player, specializations.get(slot), manager, chatPromptManager,
                    this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva especialización:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una especialización con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            chatPromptManager.prompt(player, "Escribí el id de la clase base:", baseClass -> {
                manager.save(new ClassSpecialization(id, baseClass.trim().toLowerCase(Locale.ROOT), id,
                        AscensionRequirements.none(), Map.of(), List.of(), List.of(), List.of()));
                reopen();
            });
        });
    }

    private void reopen() {
        this.specializations = List.copyOf(manager.getAll());
        open();
    }

}
