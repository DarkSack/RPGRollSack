package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GenePreviewStats;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/**
 * Elegí dos animales y previsualizá la distribución probable de la
 * genética de la cría (modo Monte Carlo, sin comprometerte a nada) antes
 * de aparearlos de verdad en el mundo — el apareamiento real sigue
 * pasando por alimentar a los dos animales, esto es solo para planificar.
 */
public class BreedingPlannerGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int SLOT_A = 2;
    private static final int SLOT_B = 6;
    private static final int PREVIEW_SLOT = 4;
    private static final int FORCE_BREED_SLOT = 8;
    private static final int GRID_START = 9;
    private static final int PER_PAGE = 27;
    private static final int PREV_SLOT = 38;
    private static final int NEXT_SLOT = 42;
    private static final int BACK_SLOT = 44;

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final GeneticsEngine geneticsEngine;
    private final PedigreeService pedigreeService;
    private final BreedingEngine breedingEngine;
    private final int inbreedingGenerations;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    private List<Animal> animals;
    private int page;
    private Animal selectedA;
    private Animal selectedB;

    public BreedingPlannerGUI(Player player, AnimalManager animalManager, SpeciesManager speciesManager,
            BreedManager breedManager, GeneManager geneManager, GeneticsEngine geneticsEngine,
            PedigreeService pedigreeService, BreedingEngine breedingEngine, int inbreedingGenerations,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.hub.breeding"), NamedTextColor.LIGHT_PURPLE), SIZE);
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.geneticsEngine = geneticsEngine;
        this.pedigreeService = pedigreeService;
        this.breedingEngine = breedingEngine;
        this.inbreedingGenerations = inbreedingGenerations;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.animals = List.copyOf(animalManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var lang = chatPromptManager.lang();

        setItem(SLOT_A, slotFor(selectedA, lang.raw("gui.planner.animal_a")));
        setItem(SLOT_B, slotFor(selectedB, lang.raw("gui.planner.animal_b")));
        setItem(PREVIEW_SLOT, new ItemBuilder(Material.HEART_OF_THE_SEA)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.planner.preview_button"), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.planner.preview_hint"), NamedTextColor.GRAY)).build());
        setItem(FORCE_BREED_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.planner.force_breed_button"), NamedTextColor.GREEN))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.planner.force_breed_hint"), NamedTextColor.GRAY))
                .build());

        int totalPages = Math.max(1, (animals.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < animals.size(); i++) {

            Animal animal = animals.get(start + i);
            Species species = speciesManager.get(animal.speciesId()).orElse(null);

            setItem(GRID_START + i, new ItemBuilder(species != null
                    ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                    : Material.BARRIER)
                    .setName(Component.text(
                            (species != null ? species.displayName() : animal.speciesId()) + " #"
                                    + animal.id().toString().substring(0, 8),
                            NamedTextColor.YELLOW))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.animal.sex_stage", "sex", animal.sex(), "stage", animal.stage()), NamedTextColor.GRAY),
                            ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.planner.pick_hint"), NamedTextColor.GRAY))
                    .build());
        }

        if (page > 0) {
            setItem(PREV_SLOT, new ItemBuilder(Material.ARROW).setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.prev"), NamedTextColor.GRAY)).build());
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW).setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.next"), NamedTextColor.GRAY)).build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    private org.bukkit.inventory.ItemStack slotFor(Animal animal, String label) {

        if (animal == null) {
            return new ItemBuilder(Material.GRAY_DYE)
                    .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.planner.slot_unselected", "label", label), NamedTextColor.GRAY)).build();
        }

        Species species = speciesManager.get(animal.speciesId()).orElse(null);

        return new ItemBuilder(species != null ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                : Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.planner.slot_selected", "label", label,
                        "id", animal.id().toString().substring(0, 8)), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.animal.sex_stage", "sex", animal.sex(), "stage", animal.stage()), NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == PREVIEW_SLOT) {
            preview();
            return;
        }

        if (slot == FORCE_BREED_SLOT) {
            forceBreed();
            return;
        }

        if (slot == PREV_SLOT) {
            page--;
            build();
            return;
        }

        if (slot == NEXT_SLOT) {
            page++;
            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
            return;
        }

        int index = page * PER_PAGE + (slot - GRID_START);

        if (slot >= GRID_START && slot < GRID_START + PER_PAGE && index < animals.size()) {

            Animal picked = animals.get(index);

            if (selectedA == null || selectedA.id().equals(picked.id())) {
                selectedA = picked;
            } else {
                selectedB = picked;
            }

            build();
        }
    }

    private void preview() {

        var lang = chatPromptManager.lang();

        if (selectedA == null || selectedB == null) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.pick_two_first"), NamedTextColor.RED));
            return;
        }

        if (!selectedA.speciesId().equals(selectedB.speciesId())) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.different_species"), NamedTextColor.RED));
            return;
        }

        if (selectedA.sex() == selectedB.sex()) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.same_sex"), NamedTextColor.RED));
            return;
        }

        List<Gene> genes = geneManager.getForSpecies(selectedA.speciesId());
        List<GenePreviewStats> stats = geneticsEngine.previewPhenotypes(selectedA.genotype(), selectedB.genotype(),
                genes, 500);

        boolean inbred = pedigreeService.isInbred(selectedA.id(), selectedA.ancestors(), selectedB.id(),
                selectedB.ancestors(), inbreedingGenerations);

        player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.preview_header"), NamedTextColor.LIGHT_PURPLE));

        for (GenePreviewStats stat : stats) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.preview_stat_line", "gene", stat.geneDisplayName(),
                    "min", String.format(Locale.ROOT, "%.1f", stat.min()), "max", String.format(Locale.ROOT, "%.1f", stat.max()),
                    "average", String.format(Locale.ROOT, "%.1f", stat.average())), NamedTextColor.AQUA));
        }

        if (inbred) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.inbreeding_warning"), NamedTextColor.RED));
        } else {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.no_relation"), NamedTextColor.GREEN));
        }
    }

    private void forceBreed() {

        var lang = chatPromptManager.lang();

        if (selectedA == null || selectedB == null) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.pick_two_first"), NamedTextColor.RED));
            return;
        }

        Entity entity = Bukkit.getEntity(selectedA.id());

        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) {
            player.sendMessage(ComponentUtils.parseWithDefault(lang.raw("gui.planner.animal_a_not_loaded"), NamedTextColor.RED));
            return;
        }

        var result = breedingEngine.attemptConception(selectedA, selectedB, livingEntity.getLocation());
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

}
