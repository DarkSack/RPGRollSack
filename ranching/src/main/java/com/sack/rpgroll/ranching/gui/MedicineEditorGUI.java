package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.MedicineType;
import com.sack.rpgroll.ranching.item.RanchingItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MedicineEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int ICON_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int TYPE_SLOT = 12;
    private static final int DISEASES_SLOT = 13;
    private static final int CURE_CHANCE_SLOT = 14;
    private static final int RECOVERY_BOOST_SLOT = 15;
    private static final int HEALTH_SLOT = 16;
    private static final int HAPPINESS_SLOT = 19;
    private static final int GIVE_SLOT = 20;
    private static final int BACK_SLOT = 40;

    private final MedicineManager medicineManager;
    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Medicine current;

    public MedicineEditorGUI(Player player, Medicine medicine, MedicineManager medicineManager,
            DiseaseManager diseaseManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Medicina: " + medicine.id(), NamedTextColor.GOLD), SIZE);
        this.current = medicine;
        this.medicineManager = medicineManager;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Medicine updated) {
        current = updated;
        medicineManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.POTION))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.BREWING_STAND)
                .setName(Component.text("Tipo: " + current.type(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para rotar", NamedTextColor.GRAY)).build());

        setItem(DISEASES_SLOT, new ItemBuilder(Material.ROTTEN_FLESH)
                .setName(Component.text("Trata: " + String.join(", ", current.curesDiseaseIds()), NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines("Enfermedades conocidas: "
                        + String.join(", ", diseaseManager.getAll().stream().map(d -> d.id()).toList())
                        + "\nEscribí ids separados por comas")).build());

        setItem(CURE_CHANCE_SLOT, new ItemBuilder(Material.GOLDEN_CARROT)
                .setName(Component.text(String.format(Locale.ROOT, "Chance de cura: %.0f%%", current.cureChance() * 100),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10% · Click derecho: -10%", NamedTextColor.GRAY)).build());

        setItem(RECOVERY_BOOST_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Reduce duración: " + current.recoveryBoostTicks() + " ticks", NamedTextColor.WHITE))
                .setLore(Component.text("Click: +600 · Click derecho: -600", NamedTextColor.GRAY)).build());

        setItem(HEALTH_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text("Bono de salud: " + current.healthBonus(), NamedTextColor.RED))
                .setLore(Component.text("Click: +2 · Click derecho: -2", NamedTextColor.GRAY)).build());

        setItem(HAPPINESS_SLOT, new ItemBuilder(Material.CAKE)
                .setName(Component.text("Bono de felicidad: " + current.happinessBonus(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click: +2 · Click derecho: -2", NamedTextColor.GRAY)).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("▶ Darme una", NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -2 : 2;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Medicine(current.id(),
                    value, current.icon(), current.description(), current.type(), current.curesDiseaseIds(),
                    current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(), current.happinessBonus())));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new Medicine(
                    current.id(), current.displayName(), value, current.description(), current.type(),
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Medicine(
                    current.id(), current.displayName(), current.icon(), value, current.type(),
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus())));
        } else if (slot == TYPE_SLOT) {
            MedicineType[] values = MedicineType.values();
            MedicineType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(), current.healthBonus(),
                    current.happinessBonus()));
        } else if (slot == DISEASES_SLOT) {
            chatPromptManager.prompt(player, "Escribí ids de enfermedad separados por comas:", value -> {

                Set<String> diseases = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        diseases.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                        current.type(), diseases, current.cureChance(), current.recoveryBoostTicks(),
                        current.healthBonus(), current.happinessBonus()));
            });
        } else if (slot == CURE_CHANCE_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), Math.max(0, Math.min(1, current.cureChance() + sign * 0.05)),
                    current.recoveryBoostTicks(), current.healthBonus(), current.happinessBonus()));
        } else if (slot == RECOVERY_BOOST_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(),
                    Math.max(0, current.recoveryBoostTicks() + (long) sign * 300), current.healthBonus(),
                    current.happinessBonus()));
        } else if (slot == HEALTH_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(),
                    Math.max(0, current.healthBonus() + sign), current.happinessBonus()));
        } else if (slot == HAPPINESS_SLOT) {
            replace(new Medicine(current.id(), current.displayName(), current.icon(), current.description(),
                    current.type(), current.curesDiseaseIds(), current.cureChance(), current.recoveryBoostTicks(),
                    current.healthBonus(), Math.max(0, current.happinessBonus() + sign)));
        } else if (slot == GIVE_SLOT) {
            player.getInventory().addItem(RanchingItemFactory.createMedicine(current));
            player.sendMessage(Component.text("✔ Te diste una medicina '" + current.displayName() + "'.", NamedTextColor.GREEN));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
