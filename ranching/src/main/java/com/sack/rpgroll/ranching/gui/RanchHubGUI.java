package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Ranch Studio — enlaza a los 7 navegadores de contenido y a las 3 pantallas de operación diaria. */
public class RanchHubGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int SPECIES_SLOT = 10;
    private static final int BREEDS_SLOT = 11;
    private static final int GENES_SLOT = 12;
    private static final int FEEDS_SLOT = 13;
    private static final int DISEASES_SLOT = 14;
    private static final int VACCINES_SLOT = 15;
    private static final int MEDICINES_SLOT = 16;

    private static final int ANIMALS_SLOT = 28;
    private static final int BREEDING_SLOT = 30;
    private static final int VETERINARY_SLOT = 32;

    private static final int CLOSE_SLOT = 40;

    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final FeedManager feedManager;
    private final DiseaseManager diseaseManager;
    private final VaccineManager vaccineManager;
    private final MedicineManager medicineManager;
    private final AnimalManager animalManager;
    private final GeneticsEngine geneticsEngine;
    private final PedigreeService pedigreeService;
    private final BreedingEngine breedingEngine;
    private final ChatPromptManager chatPromptManager;
    private final int inbreedingGenerations;

    public RanchHubGUI(Player player, SpeciesManager speciesManager, BreedManager breedManager,
            GeneManager geneManager, FeedManager feedManager, DiseaseManager diseaseManager,
            VaccineManager vaccineManager, MedicineManager medicineManager, AnimalManager animalManager,
            GeneticsEngine geneticsEngine, PedigreeService pedigreeService, BreedingEngine breedingEngine,
            ChatPromptManager chatPromptManager, int inbreedingGenerations) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.title"), NamedTextColor.GOLD), SIZE);
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.feedManager = feedManager;
        this.diseaseManager = diseaseManager;
        this.vaccineManager = vaccineManager;
        this.medicineManager = medicineManager;
        this.animalManager = animalManager;
        this.geneticsEngine = geneticsEngine;
        this.pedigreeService = pedigreeService;
        this.breedingEngine = breedingEngine;
        this.chatPromptManager = chatPromptManager;
        this.inbreedingGenerations = inbreedingGenerations;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(SPECIES_SLOT, button(Material.COW_SPAWN_EGG, chatPromptManager.lang().raw("gui.hub.species"), speciesManager.count()));
        setItem(BREEDS_SLOT, button(Material.NAME_TAG, chatPromptManager.lang().raw("gui.hub.breeds"), breedManager.count()));
        setItem(GENES_SLOT, button(Material.NETHER_STAR, chatPromptManager.lang().raw("gui.hub.genes"), geneManager.count()));
        setItem(FEEDS_SLOT, button(Material.WHEAT, chatPromptManager.lang().raw("gui.hub.feeds"), feedManager.count()));
        setItem(DISEASES_SLOT, button(Material.ROTTEN_FLESH, chatPromptManager.lang().raw("gui.hub.diseases"), diseaseManager.count()));
        setItem(VACCINES_SLOT, button(Material.POTION, chatPromptManager.lang().raw("gui.hub.vaccines"), vaccineManager.count()));
        setItem(MEDICINES_SLOT, button(Material.SPLASH_POTION, chatPromptManager.lang().raw("gui.hub.medicines"), medicineManager.count()));

        setItem(ANIMALS_SLOT, button(Material.CHEST, chatPromptManager.lang().raw("gui.hub.animals"), animalManager.getAll().size()));
        setItem(BREEDING_SLOT, new ItemBuilder(Material.HEART_OF_THE_SEA)
                .setName(Component.text(chatPromptManager.lang().raw("gui.hub.breeding"), NamedTextColor.LIGHT_PURPLE)).build());
        setItem(VETERINARY_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text(chatPromptManager.lang().raw("gui.hub.veterinary"), NamedTextColor.RED)).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.close")));
    }

    private org.bukkit.inventory.ItemStack button(Material material, String label, int count) {
        return new ItemBuilder(material)
                .setName(Component.text(label + " (" + count + ")", NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.manage_hint"), NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == SPECIES_SLOT) {
            new SpeciesBrowserGUI(player, speciesManager, chatPromptManager, this::reopen).open();
        } else if (slot == BREEDS_SLOT) {
            new BreedBrowserGUI(player, breedManager, speciesManager, chatPromptManager, this::reopen).open();
        } else if (slot == GENES_SLOT) {
            new GeneBrowserGUI(player, geneManager, chatPromptManager, this::reopen).open();
        } else if (slot == FEEDS_SLOT) {
            new FeedBrowserGUI(player, feedManager, chatPromptManager, this::reopen).open();
        } else if (slot == DISEASES_SLOT) {
            new DiseaseBrowserGUI(player, diseaseManager, chatPromptManager, this::reopen).open();
        } else if (slot == VACCINES_SLOT) {
            new VaccineBrowserGUI(player, vaccineManager, diseaseManager, chatPromptManager, this::reopen).open();
        } else if (slot == MEDICINES_SLOT) {
            new MedicineBrowserGUI(player, medicineManager, diseaseManager, chatPromptManager, this::reopen).open();
        } else if (slot == ANIMALS_SLOT) {
            new AnimalBrowserGUI(player, animalManager, speciesManager, breedManager, chatPromptManager, this::reopen).open();
        } else if (slot == BREEDING_SLOT) {
            new BreedingPlannerGUI(player, animalManager, speciesManager, breedManager, geneManager, geneticsEngine,
                    pedigreeService, breedingEngine, inbreedingGenerations, chatPromptManager, this::reopen).open();
        } else if (slot == VETERINARY_SLOT) {
            new VeterinaryGUI(player, animalManager, diseaseManager, speciesManager, chatPromptManager, this::reopen).open();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
