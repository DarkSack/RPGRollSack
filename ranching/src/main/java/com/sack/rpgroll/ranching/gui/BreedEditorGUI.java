package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

public class BreedEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int SPECIES_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int PRODUCTION_SLOT = 13;
    private static final int WEIGHT_SLOT = 14;
    private static final int FERTILITY_SLOT = 15;
    private static final int RESISTANCE_SLOT = 16;
    private static final int TEMPERAMENT_SLOT = 19;
    private static final int BACK_SLOT = 40;

    private final BreedManager breedManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Breed current;

    public BreedEditorGUI(Player player, Breed breed, BreedManager breedManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Raza: " + breed.id(), NamedTextColor.GOLD), SIZE);
        this.current = breed;
        this.breedManager = breedManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Breed updated) {
        current = updated;
        breedManager.save(current);
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

        setItem(SPECIES_SLOT, new ItemBuilder(Material.COW_SPAWN_EGG)
                .setName(Component.text("Especie: " + current.speciesId(), NamedTextColor.AQUA)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(PRODUCTION_SLOT, new ItemBuilder(Material.BUCKET)
                .setName(Component.text(String.format(Locale.ROOT, "Producción: x%.2f", current.productionMultiplier()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(Component.text(String.format(Locale.ROOT, "Peso: x%.2f", current.weightMultiplier()),
                        NamedTextColor.WHITE))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(FERTILITY_SLOT, new ItemBuilder(Material.RABBIT_FOOT)
                .setName(Component.text(String.format(Locale.ROOT, "Fertilidad: x%.2f", current.fertilityMultiplier()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(RESISTANCE_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(Component.text(String.format(Locale.ROOT, "Resistencia: x%.2f", current.resistanceMultiplier()),
                        NamedTextColor.GREEN))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(TEMPERAMENT_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(Component.text("Temperamento: " + current.temperament(), NamedTextColor.GOLD)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.1 : 0.1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Breed(current.id(),
                    value, current.description(), current.speciesId(), current.productionMultiplier(),
                    current.weightMultiplier(), current.fertilityMultiplier(), current.resistanceMultiplier(),
                    current.temperament())));
        } else if (slot == SPECIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la especie:", value -> replace(new Breed(current.id(),
                    current.displayName(), current.description(), value.trim().toLowerCase(Locale.ROOT),
                    current.productionMultiplier(), current.weightMultiplier(), current.fertilityMultiplier(),
                    current.resistanceMultiplier(), current.temperament())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Breed(current.id(),
                    current.displayName(), value, current.speciesId(), current.productionMultiplier(),
                    current.weightMultiplier(), current.fertilityMultiplier(), current.resistanceMultiplier(),
                    current.temperament())));
        } else if (slot == PRODUCTION_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    Math.max(0.1, current.productionMultiplier() + sign), current.weightMultiplier(),
                    current.fertilityMultiplier(), current.resistanceMultiplier(), current.temperament()));
        } else if (slot == WEIGHT_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), Math.max(0.1, current.weightMultiplier() + sign),
                    current.fertilityMultiplier(), current.resistanceMultiplier(), current.temperament()));
        } else if (slot == FERTILITY_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), current.weightMultiplier(),
                    Math.max(0.1, current.fertilityMultiplier() + sign), current.resistanceMultiplier(),
                    current.temperament()));
        } else if (slot == RESISTANCE_SLOT) {
            replace(new Breed(current.id(), current.displayName(), current.description(), current.speciesId(),
                    current.productionMultiplier(), current.weightMultiplier(), current.fertilityMultiplier(),
                    Math.max(0.1, current.resistanceMultiplier() + sign), current.temperament()));
        } else if (slot == TEMPERAMENT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el temperamento (ej. Calmada, Agresiva):",
                    value -> replace(new Breed(current.id(), current.displayName(), current.description(),
                            current.speciesId(), current.productionMultiplier(), current.weightMultiplier(),
                            current.fertilityMultiplier(), current.resistanceMultiplier(), value)));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
