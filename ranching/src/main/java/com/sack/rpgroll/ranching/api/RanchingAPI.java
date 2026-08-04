package com.sack.rpgroll.ranching.api;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeding.BreedingAttemptResult;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.Medicine;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.Vaccine;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.Feed;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;
import java.util.Random;

/**
 * Punto de entrada público de RPGRoll-Ranching. Además de exponer los
 * managers de contenido para que otro addon registre especies/razas/
 * genes/enfermedades/alimentos/vacunas por código
 * ({@code api.ranching().registerSpecies(...)} etc. — ver {@code
 * SpeciesManager#save} y análogos), expone acciones de cuidado
 * programáticas (alimentar/tratar/vacunar/intentar aparear) pensadas en
 * primer lugar para un futuro RPGRoll-Workers que automatice el cuidado
 * diario del rancho — hoy esas mismas acciones solo son alcanzables por
 * un jugador con click derecho.
 */
public final class RanchingAPI {

    private static RanchingAPI instance;

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
    private final Random random = new Random();

    private RanchingAPI(SpeciesManager speciesManager, BreedManager breedManager, GeneManager geneManager,
            FeedManager feedManager, DiseaseManager diseaseManager, VaccineManager vaccineManager,
            MedicineManager medicineManager, AnimalManager animalManager, GeneticsEngine geneticsEngine,
            PedigreeService pedigreeService, BreedingEngine breedingEngine) {
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
    }

    public static void init(SpeciesManager speciesManager, BreedManager breedManager, GeneManager geneManager,
            FeedManager feedManager, DiseaseManager diseaseManager, VaccineManager vaccineManager,
            MedicineManager medicineManager, AnimalManager animalManager, GeneticsEngine geneticsEngine,
            PedigreeService pedigreeService, BreedingEngine breedingEngine) {
        instance = new RanchingAPI(speciesManager, breedManager, geneManager, feedManager, diseaseManager,
                vaccineManager, medicineManager, animalManager, geneticsEngine, pedigreeService, breedingEngine);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Ranching todavía no está listo. */
    public static RanchingAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Ranching todavía no está listo.");
        }

        return instance;
    }

    // ============ Managers de contenido (registerX por código) ============

    public SpeciesManager species() {
        return speciesManager;
    }

    public BreedManager breeds() {
        return breedManager;
    }

    public GeneManager genes() {
        return geneManager;
    }

    public FeedManager feeds() {
        return feedManager;
    }

    public DiseaseManager diseases() {
        return diseaseManager;
    }

    public VaccineManager vaccines() {
        return vaccineManager;
    }

    public MedicineManager medicines() {
        return medicineManager;
    }

    // ============ Consulta de animales ============

    public Optional<Animal> getAnimal(Entity entity) {
        return animalManager.resolve(entity);
    }

    public java.util.Collection<Animal> getAllAnimals() {
        return animalManager.getAll();
    }

    // ============ Cuidado programático (pensado para RPGRoll-Workers) ============

    /** @return false si el alimento no existe o el animal no acepta cuidado programático por alguna razón futura. */
    public boolean feedAnimal(Animal animal, String feedId) {

        Optional<Feed> feed = feedManager.get(feedId);

        if (feed.isEmpty()) {
            return false;
        }

        Species species = speciesManager.get(animal.speciesId()).orElse(null);
        boolean matchesDiet = species == null || feed.get().satisfiesDiet(species.dietTags());
        double effectiveness = matchesDiet ? 1.0 : 0.4;

        animal.setHealth(animal.health() + feed.get().healthBonus() * effectiveness);
        animal.setHappiness(animal.happiness() + feed.get().happinessBonus() * effectiveness);
        animal.addProductionBonus(feed.get().productionBonus() * feed.get().quality().multiplier() * effectiveness);
        animalManager.save(animal);

        return true;
    }

    /** @return true si curó al animal (instantáneo o por chance), false si no aplicaba o no prendió. */
    public boolean treatAnimal(Animal animal, String medicineId) {

        Optional<Medicine> medicine = medicineManager.get(medicineId);

        if (medicine.isEmpty() || !animal.isSick() || !medicine.get().curesDiseaseIds().contains(animal.activeDiseaseId())) {
            return false;
        }

        animal.setHealth(animal.health() + medicine.get().healthBonus());
        animal.setHappiness(animal.happiness() + medicine.get().happinessBonus());

        boolean cured = random.nextDouble() < medicine.get().cureChance();

        if (cured) {
            animal.cure();
        } else {
            animal.reduceDiseaseDuration(medicine.get().recoveryBoostTicks());
        }

        animalManager.save(animal);
        return cured;
    }

    public boolean vaccinateAnimal(Animal animal, String vaccineId) {

        Optional<Vaccine> vaccine = vaccineManager.get(vaccineId);

        if (vaccine.isEmpty()) {
            return false;
        }

        for (String diseaseId : vaccine.get().preventsDiseaseIds()) {
            animal.grantImmunity(diseaseId, vaccine.get().immunityDurationTicks(), 1.0 - vaccine.get().riskReduction());
        }

        animalManager.save(animal);
        return true;
    }

    public BreedingAttemptResult attemptBreeding(Animal first, Animal second, Location location) {
        return breedingEngine.attemptConception(first, second, location);
    }

}
