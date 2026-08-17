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
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public AnimalDetailGUI(Player player, Animal animal, SpeciesManager speciesManager, BreedManager breedManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.animal.detail_title",
                "id", animal.id().toString().substring(0, 8)), NamedTextColor.GOLD), SIZE);
        this.animal = animal;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.chatPromptManager = chatPromptManager;
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

        var lang = chatPromptManager.lang();

        setItem(IDENTITY_SLOT, new ItemBuilder(species != null
                ? SpeciesBrowserGUI.parseMaterial(species.icon(), Material.COW_SPAWN_EGG)
                : Material.BARRIER)
                .setName(Component.text(lang.raw("gui.animal.detail.identity_title"), NamedTextColor.GOLD))
                .setLore(ItemBuilder.toLoreLines(
                        lang.raw("gui.animal.detail.species", "species", species != null ? species.displayName() : animal.speciesId()) + "\n"
                                + lang.raw("gui.animal.detail.breed", "breed", breed != null ? breed.displayName() : lang.raw("gui.animal.detail.no_breed")) + "\n"
                                + lang.raw("gui.animal.sex", "sex", animal.sex()) + "\n"
                                + lang.raw("gui.animal.stage", "stage", animal.stage()) + "\n"
                                + lang.raw("gui.animal.detail.weight", "weight", String.format(Locale.ROOT, "%.1f", animal.weight())) + "\n"
                                + lang.raw("gui.animal.generation", "gen", animal.generation()) + "\n"
                                + lang.raw("gui.animal.quality", "quality", animal.quality())
                                + (!animal.mutationTags().isEmpty()
                                        ? "\n" + lang.raw("gui.animal.detail.mutations", "mutations", String.join(", ", animal.mutationTags()))
                                        : "")))
                .build());

        setItem(GENETICS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text(lang.raw("gui.animal.detail.genetics_title"), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(formatPhenotype()))
                .build());

        setItem(LINEAGE_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text(lang.raw("gui.animal.detail.lineage_title"), NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(formatLineage()))
                .build());

        setItem(STATUS_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text(lang.raw("gui.animal.detail.status_title"), NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(
                        lang.raw("gui.animal.detail.health_line", "health", String.format(Locale.ROOT, "%.0f", animal.health())) + "\n"
                                + lang.raw("gui.animal.detail.happiness_line", "happiness", String.format(Locale.ROOT, "%.0f", animal.happiness())) + "\n"
                                + lang.raw("gui.animal.detail.fertility_line", "fertility", String.format(Locale.ROOT, "%.0f", animal.fertility() * 100)) + "\n"
                                + (animal.isPregnant()
                                        ? lang.raw("gui.animal.detail.pregnant_line", "ticks", Math.max(0, animal.pregnancyRemainingTicks())) + "\n"
                                        : "")
                                + (animal.isSick() ? lang.raw("gui.animal.detail.sick_line", "disease", animal.activeDiseaseId(),
                                        "ticks", animal.diseaseRemainingTicks()) : lang.raw("gui.animal.detail.healthy"))))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String formatPhenotype() {

        if (animal.phenotype().isEmpty()) {
            return chatPromptManager.lang().raw("gui.animal.detail.no_genes");
        }

        StringBuilder builder = new StringBuilder();

        for (var entry : animal.phenotype().entrySet()) {
            builder.append(entry.getKey()).append(": ").append(String.format(Locale.ROOT, "%.1f", entry.getValue()))
                    .append("\n");
        }

        return builder.toString().strip();
    }

    private String formatLineage() {

        var lang = chatPromptManager.lang();

        if (animal.isFounder()) {
            return lang.raw("gui.animal.detail.founder");
        }

        List<AncestorRef> ancestors = animal.ancestors();

        if (ancestors.isEmpty()) {
            return lang.raw("gui.animal.detail.no_lineage");
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < ancestors.size() && i < 10; i++) {
            AncestorRef ref = ancestors.get(i);
            String key = i < 2 ? (i == 0 ? "gui.animal.detail.mother" : "gui.animal.detail.father") : "gui.animal.detail.ancestor";
            builder.append(lang.raw(key, "name", ref.displayName())).append("\n");
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
