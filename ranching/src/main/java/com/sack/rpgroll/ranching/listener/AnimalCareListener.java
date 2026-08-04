package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.Vaccine;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.Feed;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.item.RanchingItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Click derecho en un animal rastreado con un alimento/medicina/vacuna en
 * la mano principal — consume 1 del item y aplica su efecto. No hay una
 * mecánica de "recolectar producción" acá: eso lo maneja {@code
 * ProductionEngine} sobre la interacción vanilla normal de cada especie
 * (ordeñar, esquilar, etc.), este listener es solo cuidado del animal.
 */
public class AnimalCareListener implements Listener {

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final FeedManager feedManager;
    private final MedicineManager medicineManager;
    private final VaccineManager vaccineManager;
    private final DiseaseManager diseaseManager;
    private final Random random = new Random();

    public AnimalCareListener(AnimalManager animalManager, SpeciesManager speciesManager, FeedManager feedManager,
            MedicineManager medicineManager, VaccineManager vaccineManager, DiseaseManager diseaseManager) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.feedManager = feedManager;
        this.medicineManager = medicineManager;
        this.vaccineManager = vaccineManager;
        this.diseaseManager = diseaseManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        var animalOptional = animalManager.resolve(event.getRightClicked());

        if (animalOptional.isEmpty()) {
            return;
        }

        Animal animal = animalOptional.get();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String feedId = RanchingItemFactory.getFeedId(item);
        if (feedId != null) {
            event.setCancelled(true);
            feedManager.get(feedId).ifPresent(feed -> handleFeed(player, animal, feed, item));
            return;
        }

        String medicineId = RanchingItemFactory.getMedicineId(item);
        if (medicineId != null) {
            event.setCancelled(true);
            medicineManager.get(medicineId).ifPresent(medicine -> handleMedicine(player, animal, medicine, item));
            return;
        }

        String vaccineId = RanchingItemFactory.getVaccineId(item);
        if (vaccineId != null) {
            event.setCancelled(true);
            vaccineManager.get(vaccineId).ifPresent(vaccine -> handleVaccine(player, animal, vaccine, item));
        }
    }

    private void handleFeed(Player player, Animal animal, Feed feed, ItemStack item) {

        Species species = speciesManager.get(animal.speciesId()).orElse(null);
        boolean matchesDiet = species == null || feed.satisfiesDiet(species.dietTags());
        double effectiveness = matchesDiet ? 1.0 : 0.4;

        animal.setHealth(animal.health() + feed.healthBonus() * effectiveness);
        animal.setHappiness(animal.happiness() + feed.happinessBonus() * effectiveness);
        animal.addProductionBonus(feed.productionBonus() * feed.quality().multiplier() * effectiveness);

        consumeOne(item);

        player.sendMessage(Component.text(
                matchesDiet ? "✔ Alimentado con " + feed.displayName() + "."
                        : "Alimentado con " + feed.displayName() + " — no es lo que más le gusta a esta especie.",
                matchesDiet ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
    }

    private void handleMedicine(Player player, Animal animal, Medicine medicine, ItemStack item) {

        animal.setHealth(animal.health() + medicine.healthBonus());
        animal.setHappiness(animal.happiness() + medicine.happinessBonus());
        consumeOne(item);

        if (!animal.isSick()) {
            player.sendMessage(Component.text("Este animal no está enfermo — igual recibió los bonos de la medicina.",
                    NamedTextColor.GRAY));
            return;
        }

        if (!medicine.curesDiseaseIds().contains(animal.activeDiseaseId())) {
            player.sendMessage(Component.text("Esta medicina no trata la enfermedad que tiene este animal.",
                    NamedTextColor.YELLOW));
            return;
        }

        if (random.nextDouble() < medicine.cureChance()) {

            Disease disease = diseaseManager.get(animal.activeDiseaseId()).orElse(null);
            animal.cure();

            player.sendMessage(Component.text(
                    "✔ ¡Curado! " + (disease != null ? disease.displayName() : "La enfermedad") + " fue tratada.",
                    NamedTextColor.GREEN));

        } else {

            animal.reduceDiseaseDuration(medicine.recoveryBoostTicks());
            player.sendMessage(Component.text("El tratamiento no curó del todo, pero acortó la recuperación.",
                    NamedTextColor.YELLOW));
        }
    }

    private void handleVaccine(Player player, Animal animal, Vaccine vaccine, ItemStack item) {

        for (String diseaseId : vaccine.preventsDiseaseIds()) {
            animal.grantImmunity(diseaseId, vaccine.immunityDurationTicks(), 1.0 - vaccine.riskReduction());
        }

        consumeOne(item);

        player.sendMessage(Component.text("✔ Vacunado con " + vaccine.displayName() + ".", NamedTextColor.GREEN));
    }

    private void consumeOne(ItemStack item) {
        item.setAmount(item.getAmount() - 1);
    }

}
