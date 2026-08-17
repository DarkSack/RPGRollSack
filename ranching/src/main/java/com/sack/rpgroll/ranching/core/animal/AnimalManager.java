package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.common.reskin.EntityReskin;
import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Registro en memoria de todos los animales rastreados — se carga entero
 * al arrancar (un rancho de tamaño razonable entra cómodo en memoria,
 * mismo criterio que el resto de los addons de este proyecto) y se
 * persiste por animal vía {@link AnimalStore}.
 */
public class AnimalManager {

    private final Plugin plugin;
    private final AnimalStore store;
    private final Map<UUID, Animal> animals = new HashMap<>();
    private final Random random = new Random();

    public AnimalManager(Plugin plugin) {
        this.plugin = plugin;
        this.store = new AnimalStore(plugin);
    }

    public void loadAll() {

        animals.clear();

        for (Animal animal : store.loadAll()) {
            animals.put(animal.id(), animal);
        }

        plugin.getLogger().info("✔ Animales cargados: " + animals.size());
    }

    public void saveAll() {
        animals.values().forEach(store::save);
    }

    public void save(Animal animal) {
        store.save(animal);
    }

    public Optional<Animal> get(UUID id) {
        return Optional.ofNullable(animals.get(id));
    }

    public Collection<Animal> getAll() {
        return animals.values();
    }

    public List<Animal> getBySpecies(String speciesId) {
        return animals.values().stream().filter(animal -> animal.speciesId().equals(speciesId)).toList();
    }

    public void remove(UUID id) {
        animals.remove(id);
        store.delete(id);
    }

    /** Marca una entidad ya spawneada (vanilla o de otro plugin) como un animal fundador (F0), sin padres. */
    public Animal registerFounder(LivingEntity entity, Species species, Breed breed, Sex sex,
            GeneticsEngine geneticsEngine, List<Gene> genesForSpecies) {

        Map<String, com.sack.rpgroll.ranching.core.genetics.AllelePair> genotype = geneticsEngine
                .createFounderGenotype(genesForSpecies);

        Map<String, Double> phenotype = new HashMap<>();
        for (Gene gene : genesForSpecies) {
            phenotype.put(gene.id(), gene.clamp(genotype.get(gene.id())
                    .resolve(gene.dominance())));
        }

        double weight = species.baseWeightMin() + random.nextDouble() * (species.baseWeightMax() - species.baseWeightMin());
        weight *= breed != null ? breed.weightMultiplier() : 1.0;

        Animal animal = new Animal(entity.getUniqueId(), species.id(), breed != null ? breed.id() : null, sex,
                genotype, phenotype, List.of(), null, null, List.of(), 0, weight, System.currentTimeMillis());

        animal.setStage(GrowthStage.ADULT);
        animal.setFertility(species.baseFertility() * (breed != null ? breed.fertilityMultiplier() : 1.0));

        applyAppearance(entity, breed, GrowthStage.ADULT);
        tagEntity(entity, species.id());
        animals.put(animal.id(), animal);
        store.save(animal);

        return animal;
    }

    /** Registra una cría ya nacida (spawneada por {@code PregnancyManager}) con su genética ya resuelta. */
    public void registerNewborn(LivingEntity entity, Animal animal) {
        tagEntity(entity, animal.speciesId());
        animals.put(animal.id(), animal);
        store.save(animal);
    }

    public void tagEntity(Entity entity, String speciesId) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(AnimalKeys.TRACKED, PersistentDataType.BOOLEAN, true);
        pdc.set(AnimalKeys.SPECIES_ID, PersistentDataType.STRING, speciesId);
    }

    public boolean isTracked(Entity entity) {
        return entity.getPersistentDataContainer().has(AnimalKeys.TRACKED, PersistentDataType.BOOLEAN);
    }

    public Optional<Animal> resolve(Entity entity) {
        return isTracked(entity) ? get(entity.getUniqueId()) : Optional.empty();
    }

    /**
     * Aplica (o remueve) el reskin visual propio de la raza — punto centralizado
     * que reemplaza la lógica repetida en cada call-site de spawn. La escala del
     * display se reduce en la etapa BABY, igual que ya hace vanilla vía Ageable.
     */
    public void applyAppearance(LivingEntity entity, Breed breed, GrowthStage stage) {

        EntityReskin reskin = breed != null ? breed.reskin() : EntityReskin.NONE;

        if (!reskin.isActive()) {
            EntityReskinService.apply(plugin, entity, EntityReskin.NONE);
            return;
        }

        double scaleFactor = stage == GrowthStage.BABY ? 0.5 : 1.0;
        EntityReskinService.apply(plugin, entity,
                new EntityReskin(reskin.material(), reskin.customModelData(), reskin.scale() * scaleFactor, reskin.yOffset()));
    }

    /** Auto-sanado barato del passenger de reskin — para llamar desde el tick periódico de crecimiento. */
    public void ensureAppearanceAttached(LivingEntity entity, Breed breed, GrowthStage stage) {

        EntityReskin reskin = breed != null ? breed.reskin() : EntityReskin.NONE;

        if (!reskin.isActive()) {
            return;
        }

        double scaleFactor = stage == GrowthStage.BABY ? 0.5 : 1.0;
        EntityReskinService.ensureAttached(plugin, entity,
                new EntityReskin(reskin.material(), reskin.customModelData(), reskin.scale() * scaleFactor, reskin.yOffset()));
    }

    public EntityType resolveEntityType(Species species) {

        try {
            return EntityType.valueOf(species.entityType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ '" + species.entityType() + "' no es un EntityType válido para la especie '"
                    + species.id() + "' — usando COW.");
            return EntityType.COW;
        }
    }

}
