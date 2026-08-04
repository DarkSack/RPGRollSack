package com.sack.rpgroll.ranching.core.breeding;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.animal.PendingOffspring;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.genetics.BreedingOutcome;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Orquesta un intento de apareamiento entre dos animales rastreados —
 * valida especie/sexo/etapa/embarazo, resuelve la genética de la cría con
 * {@link GeneticsEngine} (congelándola ya en el momento de la concepción,
 * no al nacer — así el nacimiento no depende de que el padre siga vivo),
 * y o bien la gesta ({@code Species#gestates()}) o la spawnea directo si
 * la especie no tiene gestación configurada.
 */
public class BreedingEngine {

    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final GeneticsEngine geneticsEngine;
    private final PedigreeService pedigreeService;
    private final AnimalManager animalManager;
    private final int inbreedingGenerations;
    private final Random random = new Random();

    public BreedingEngine(SpeciesManager speciesManager, BreedManager breedManager, GeneManager geneManager,
            GeneticsEngine geneticsEngine, PedigreeService pedigreeService, AnimalManager animalManager,
            int inbreedingGenerations) {
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.geneticsEngine = geneticsEngine;
        this.pedigreeService = pedigreeService;
        this.animalManager = animalManager;
        this.inbreedingGenerations = inbreedingGenerations;
    }

    public BreedingAttemptResult attemptConception(Animal first, Animal second, Location location) {

        if (!first.speciesId().equals(second.speciesId())) {
            return BreedingAttemptResult.fail("Esos dos no son de la misma especie.");
        }

        if (first.sex() == second.sex()) {
            return BreedingAttemptResult.fail("Esos dos son del mismo sexo — no pueden reproducirse.");
        }

        Animal mother = first.sex() == Sex.FEMALE ? first : second;
        Animal father = first.sex() == Sex.MALE ? first : second;

        if (mother.stage() != GrowthStage.ADULT || father.stage() != GrowthStage.ADULT) {
            return BreedingAttemptResult.fail("Los dos tienen que ser adultos para reproducirse.");
        }

        if (mother.isPregnant()) {
            return BreedingAttemptResult.fail("Esa hembra ya está preñada.");
        }

        Species species = speciesManager.get(mother.speciesId()).orElse(null);

        if (species == null) {
            return BreedingAttemptResult.fail("La especie de estos animales ya no existe.");
        }

        if (random.nextDouble() > mother.fertility()) {
            return BreedingAttemptResult.fail("El apareamiento no prendió esta vez.");
        }

        boolean inbred = pedigreeService.isInbred(mother.id(), mother.ancestors(), father.id(), father.ancestors(),
                inbreedingGenerations);

        List<Gene> genes = geneManager.getForSpecies(species.id());
        int litterSize = species.minLitterSize()
                + (species.maxLitterSize() > species.minLitterSize()
                        ? random.nextInt(species.maxLitterSize() - species.minLitterSize() + 1)
                        : 0);

        List<AncestorRef> ancestry = pedigreeService.buildAncestry(ancestorRef(mother), ancestorRef(father),
                mother.ancestors(), father.ancestors());
        int generation = Math.max(mother.generation(), father.generation()) + 1;

        List<PendingOffspring> litter = new ArrayList<>();

        for (int i = 0; i < litterSize; i++) {

            BreedingOutcome outcome = geneticsEngine.breed(mother.genotype(), father.genotype(), genes);
            Sex offspringSex = random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
            String breedId = random.nextBoolean() ? mother.breedId() : father.breedId();

            litter.add(new PendingOffspring(outcome, offspringSex, breedId, generation, ancestry, mother.id(),
                    father.id()));
        }

        String inbreedingNote = inbred ? " ⚠ Cría con endogamia — la calidad genética puede resentirse." : "";

        if (!species.gestates()) {

            for (PendingOffspring pending : litter) {
                spawnOffspring(pending, species, location);
            }

            return BreedingAttemptResult.ok("✔ ¡Nació una cría al instante!" + inbreedingNote);
        }

        mother.startPregnancy(species.gestationDurationTicks(), litter);
        animalManager.save(mother);

        return BreedingAttemptResult.ok("✔ ¡Apareamiento exitoso! Gestación en curso." + inbreedingNote);
    }

    /** Llamado por {@code PregnancyTask} cuando una gestación llega a término. */
    public void birthPending(Animal mother, Location location) {

        Species species = speciesManager.get(mother.speciesId()).orElse(null);
        List<PendingOffspring> litter = mother.finishPregnancy();

        if (species == null) {
            return;
        }

        for (PendingOffspring pending : litter) {
            spawnOffspring(pending, species, location);
        }

        animalManager.save(mother);
    }

    private void spawnOffspring(PendingOffspring pending, Species species, Location location) {

        World world = location.getWorld();

        if (world == null) {
            return;
        }

        LivingEntity entity = (LivingEntity) world.spawnEntity(location, animalManager.resolveEntityType(species));

        if (entity instanceof Ageable ageable) {
            ageable.setBaby();
        }

        double weight = species.baseWeightMin();
        Breed breed = pending.breedId() != null ? breedManager.get(pending.breedId()).orElse(null) : null;

        if (breed != null) {
            weight *= breed.weightMultiplier();
        }

        Animal newborn = new Animal(entity.getUniqueId(), species.id(), pending.breedId(), pending.sex(),
                pending.outcome().genotype(), pending.outcome().phenotype(), extractCosmeticTags(pending),
                pending.motherId(), pending.fatherId(), pending.ancestry(), pending.generation(), weight,
                System.currentTimeMillis());

        newborn.setStage(GrowthStage.BABY);
        newborn.setFertility(species.baseFertility() * (breed != null ? breed.fertilityMultiplier() : 1.0));

        animalManager.registerNewborn(entity, newborn);
    }

    private List<String> extractCosmeticTags(PendingOffspring pending) {

        List<String> tags = new ArrayList<>();

        for (var mutation : pending.outcome().triggeredMutations()) {
            if (mutation.effectType() == com.sack.rpgroll.ranching.core.genetics.MutationEffectType.COSMETIC_TAG) {
                tags.add(mutation.id());
            }
        }

        return tags;
    }

    private AncestorRef ancestorRef(Animal animal) {

        Breed breed = animal.breedId() != null ? breedManager.get(animal.breedId()).orElse(null) : null;
        Species species = speciesManager.get(animal.speciesId()).orElse(null);

        String label = breed != null ? breed.displayName() : species != null ? species.displayName() : animal.speciesId();

        return new AncestorRef(animal.id(), label, animal.speciesId());
    }

}
