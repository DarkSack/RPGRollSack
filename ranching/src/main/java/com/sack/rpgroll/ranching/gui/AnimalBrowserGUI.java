package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Lista todos los animales rastreados, paginada — click en uno abre su ficha completa (genética, linaje, salud). */
public class AnimalBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int PER_PAGE = 36;
    private static final int PREV_SLOT = 38;
    private static final int NEXT_SLOT = 42;
    private static final int BACK_SLOT = 44;

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final Runnable onBack;
    private List<Animal> animals;
    private int page;

    public AnimalBrowserGUI(Player player, AnimalManager animalManager, SpeciesManager speciesManager,
            BreedManager breedManager, Runnable onBack) {
        super(player, Component.text("Explorador de Animales", NamedTextColor.GOLD), SIZE);
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.onBack = onBack;
        this.animals = List.copyOf(animalManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        int totalPages = Math.max(1, (animals.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < animals.size(); i++) {

            Animal animal = animals.get(start + i);
            Species species = speciesManager.get(animal.speciesId()).orElse(null);

            setItem(i, new ItemBuilder(species != null
                    ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                    : Material.BARRIER)
                    .setName(Component.text(
                            (species != null ? species.displayName() : animal.speciesId()) + " #"
                                    + animal.id().toString().substring(0, 8),
                            NamedTextColor.YELLOW))
                    .setLore(
                            Component.text("Sexo: " + animal.sex(), NamedTextColor.GRAY),
                            Component.text("Etapa: " + animal.stage(), NamedTextColor.GRAY),
                            Component.text("Calidad: " + animal.quality(), NamedTextColor.GOLD),
                            Component.text("Generación: F" + animal.generation(), NamedTextColor.GRAY),
                            Component.text(String.format(Locale.ROOT, "Salud: %.0f · Felicidad: %.0f",
                                    animal.health(), animal.happiness()), NamedTextColor.GREEN),
                            animal.isSick()
                                    ? Component.text("⚠ Enfermo: " + animal.activeDiseaseId(), NamedTextColor.RED)
                                    : Component.text("Sano", NamedTextColor.GREEN),
                            animal.isPregnant() ? Component.text("Preñada", NamedTextColor.LIGHT_PURPLE)
                                    : Component.text(""))
                    .build());
        }

        if (page > 0) {
            setItem(PREV_SLOT, new ItemBuilder(Material.ARROW)
                    .setName(Component.text("Página anterior", NamedTextColor.GRAY)).build());
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .setName(Component.text("Página siguiente", NamedTextColor.GRAY)).build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int index = page * PER_PAGE + slot;

        if (slot < PER_PAGE && index < animals.size()) {
            new AnimalDetailGUI(player, animals.get(index), speciesManager, breedManager, this::reopen).open();
        } else if (slot == PREV_SLOT) {
            page--;
            build();
        } else if (slot == NEXT_SLOT) {
            page++;
            build();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        this.animals = List.copyOf(animalManager.getAll());
        open();
    }

}
