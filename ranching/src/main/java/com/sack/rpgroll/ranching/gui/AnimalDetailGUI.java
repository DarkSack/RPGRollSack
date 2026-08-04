package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Ficha de solo lectura de UN animal — genética expresada, linaje congelado, y estado de salud/bienestar. */
public class AnimalDetailGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int IDENTITY_SLOT = 10;
    private static final int GENETICS_SLOT = 12;
    private static final int LINEAGE_SLOT = 14;
    private static final int STATUS_SLOT = 16;
    private static final int BACK_SLOT = 40;

    private final Animal animal;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final Runnable onBack;

    public AnimalDetailGUI(Player player, Animal animal, SpeciesManager speciesManager, BreedManager breedManager,
            Runnable onBack) {
        super(player, Component.text("Animal #" + animal.id().toString().substring(0, 8), NamedTextColor.GOLD), SIZE);
        this.animal = animal;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        Species species = speciesManager.get(animal.speciesId()).orElse(null);
        Breed breed = animal.breedId() != null ? breedManager.get(animal.breedId()).orElse(null) : null;

        setItem(IDENTITY_SLOT, new ItemBuilder(species != null
                ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                : Material.BARRIER)
                .setName(Component.text("Identidad", NamedTextColor.GOLD))
                .setLore(ItemBuilder.toLoreLines(
                        "Especie: " + (species != null ? species.displayName() : animal.speciesId()) + "\n"
                                + "Raza: " + (breed != null ? breed.displayName() : "(sin raza)") + "\n"
                                + "Sexo: " + animal.sex() + "\n"
                                + "Etapa: " + animal.stage() + "\n"
                                + String.format(Locale.ROOT, "Peso: %.1f kg", animal.weight()) + "\n"
                                + "Generación: F" + animal.generation() + "\n"
                                + "Calidad: " + animal.quality()
                                + (!animal.mutationTags().isEmpty()
                                        ? "\nMutaciones: " + String.join(", ", animal.mutationTags())
                                        : "")))
                .build());

        setItem(GENETICS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Genética expresada", NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(formatPhenotype()))
                .build());

        setItem(LINEAGE_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Linaje", NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(formatLineage()))
                .build());

        setItem(STATUS_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text("Estado", NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(
                        String.format(Locale.ROOT, "Salud: %.0f/100\n", animal.health())
                                + String.format(Locale.ROOT, "Felicidad: %.0f/100\n", animal.happiness())
                                + String.format(Locale.ROOT, "Fertilidad: %.0f%%\n", animal.fertility() * 100)
                                + (animal.isPregnant()
                                        ? "Preñada — faltan " + Math.max(0, animal.pregnancyRemainingTicks()) + " ticks\n"
                                        : "")
                                + (animal.isSick() ? "⚠ Enferma: " + animal.activeDiseaseId() + " ("
                                        + animal.diseaseRemainingTicks() + " ticks restantes)" : "Sana")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String formatPhenotype() {

        if (animal.phenotype().isEmpty()) {
            return "(sin genes registrados)";
        }

        StringBuilder builder = new StringBuilder();

        for (var entry : animal.phenotype().entrySet()) {
            builder.append(entry.getKey()).append(": ").append(String.format(Locale.ROOT, "%.1f", entry.getValue()))
                    .append("\n");
        }

        return builder.toString().strip();
    }

    private String formatLineage() {

        if (animal.isFounder()) {
            return "Fundador de línea (sin padres registrados).";
        }

        List<AncestorRef> ancestors = animal.ancestors();

        if (ancestors.isEmpty()) {
            return "(sin linaje registrado)";
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < ancestors.size() && i < 10; i++) {
            AncestorRef ref = ancestors.get(i);
            builder.append(i < 2 ? (i == 0 ? "Madre: " : "Padre: ") : "Ancestro: ").append(ref.displayName())
                    .append("\n");
        }

        return builder.toString().strip();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (event.getSlot() == BACK_SLOT) {
            onBack.run();
        }
    }

}
