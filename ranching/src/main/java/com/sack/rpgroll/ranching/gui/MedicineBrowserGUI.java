package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.MedicineType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MedicineBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MedicineManager medicineManager;
    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Medicine> medicines;

    public MedicineBrowserGUI(Player player, MedicineManager medicineManager, DiseaseManager diseaseManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Medicinas", NamedTextColor.GOLD), SIZE);
        this.medicineManager = medicineManager;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.medicines = List.copyOf(medicineManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < medicines.size() && i < 36; i++) {

            Medicine medicine = medicines.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(medicine.icon(), Material.POTION))
                    .setName(Component.text(medicine.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + medicine.id(), NamedTextColor.GRAY),
                            Component.text("tipo: " + medicine.type(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear medicina nueva", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < medicines.size() && slot < 36) {
            new MedicineEditorGUI(player, medicines.get(slot), medicineManager, diseaseManager, chatPromptManager,
                    this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva medicina:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (medicineManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una medicina con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            medicineManager.save(new Medicine(id, id, "POTION", "", MedicineType.VITAMIN, Set.of(), 0.5, 0, 0, 0));
            reopen();
        });
    }

    private void reopen() {
        this.medicines = List.copyOf(medicineManager.getAll());
        build();
    }

}
