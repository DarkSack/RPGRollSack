package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DiseaseBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Disease> diseases;

    public DiseaseBrowserGUI(Player player, DiseaseManager diseaseManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Enfermedades", NamedTextColor.GOLD), SIZE);
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.diseases = List.copyOf(diseaseManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < diseases.size() && i < 36; i++) {

            Disease disease = diseases.get(i);

            setItem(i, new ItemBuilder(Material.ROTTEN_FLESH)
                    .setName(Component.text(disease.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + disease.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear enfermedad nueva", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < diseases.size() && slot < 36) {
            new DiseaseEditorGUI(player, diseases.get(slot), diseaseManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva enfermedad:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (diseaseManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una enfermedad con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            diseaseManager.save(new Disease(id, id, "", Set.of(), 6000, 0.05, 2, 2, 0.5));
            reopen();
        });
    }

    private void reopen() {
        this.diseases = List.copyOf(diseaseManager.getAll());
        open();
    }

}
