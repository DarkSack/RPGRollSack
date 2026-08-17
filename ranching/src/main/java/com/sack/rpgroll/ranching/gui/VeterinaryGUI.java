package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Panel administrativo: lista solo los animales enfermos y permite curarlos de un click. */
public class VeterinaryGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int BACK_SLOT = 44;

    private final AnimalManager animalManager;
    private final DiseaseManager diseaseManager;
    private final SpeciesManager speciesManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Animal> sickAnimals;

    public VeterinaryGUI(Player player, AnimalManager animalManager, DiseaseManager diseaseManager,
            SpeciesManager speciesManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.veterinary"), NamedTextColor.RED), SIZE);
        this.animalManager = animalManager;
        this.diseaseManager = diseaseManager;
        this.speciesManager = speciesManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.sickAnimals = animalManager.getAll().stream().filter(Animal::isSick).toList();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        if (sickAnimals.isEmpty()) {
            setItem(22, new ItemBuilder(Material.LIME_DYE)
                    .setName(Component.text(chatPromptManager.lang().raw("gui.veterinary.none_sick"), NamedTextColor.GREEN)).build());
        }

        for (int i = 0; i < sickAnimals.size() && i < 36; i++) {

            Animal animal = sickAnimals.get(i);
            Disease disease = diseaseManager.get(animal.activeDiseaseId()).orElse(null);
            Species species = speciesManager.get(animal.speciesId()).orElse(null);

            setItem(i, new ItemBuilder(Material.ROTTEN_FLESH)
                    .setName(Component.text(
                            (species != null ? species.displayName() : animal.speciesId()) + " #"
                                    + animal.id().toString().substring(0, 8),
                            NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.veterinary.disease", "disease",
                                    disease != null ? disease.displayName() : animal.activeDiseaseId()),
                                    NamedTextColor.RED),
                            Component.text(chatPromptManager.lang().raw("gui.veterinary.ticks_remaining", "ticks",
                                    animal.diseaseRemainingTicks()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.veterinary.click_to_cure"), NamedTextColor.GREEN))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < sickAnimals.size() && slot < 36) {

            Animal animal = sickAnimals.get(slot);
            animal.cure();
            animalManager.save(animal);
            player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.veterinary.cured"), NamedTextColor.GREEN));
            reopen();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        this.sickAnimals = animalManager.getAll().stream().filter(Animal::isSick).toList();
        open();
    }

}
