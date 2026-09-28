package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.common.reskin.EntityReskin;
import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.integration.ModelsIntegration;
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

    /** Autoguardado periódico sin frenar el servidor (ver {@link AnimalStore#saveAllAsync}). */
    public void autosave() {
        store.saveAllAsync(animals.values());
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

    /** Los animales de un jugador, en un orden estable (especie y luego id). */
    public List<Animal> ownedBy(UUID ownerId) {
        return animals.values().stream().filter(animal -> animal.isOwnedBy(ownerId))
                .sorted(java.util.Comparator.comparing(Animal::speciesId).thenComparing(Animal::id)).toList();
    }

    public List<Animal> forSale() {
        return animals.values().stream().filter(Animal::isForSale)
                .sorted(java.util.Comparator.comparingDouble(Animal::salePrice)).toList();
    }

    /** Busca por el principio del id (lo que se enseña en los menús y en el chat). */
    public Optional<Animal> byShortId(String prefix) {
        String lower = prefix.toLowerCase(java.util.Locale.ROOT);
        List<Animal> matches = animals.values().stream().filter(a -> a.id().toString().startsWith(lower)).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    /** La entidad que lo representa, si está cargada. */
    public Optional<Entity> entityOf(Animal animal) {
        Entity entity = org.bukkit.Bukkit.getEntity(animal.entityId());
        return entity != null && entity.isValid() ? Optional.of(entity) : Optional.empty();
    }

    /** Une una entidad al animal: la marca y recuerda su uuid y dónde está. */
    public void bindEntity(Entity entity, Animal animal) {
        tagEntity(entity, animal.speciesId());
        entity.getPersistentDataContainer().set(AnimalKeys.ANIMAL_ID, PersistentDataType.STRING, animal.id().toString());
        animal.setEntityId(entity.getUniqueId());
        updateLastSeen(animal, entity.getLocation());
    }

    public void updateLastSeen(Animal animal, Location location) {
        if (location.getWorld() != null) {
            animal.setLastSeen(new Animal.LastSeen(location.getWorld().getName(), location.getX(), location.getY(),
                    location.getZ()));
        }
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
        return registerFounder(entity, species, breed, sex, geneticsEngine, genesForSpecies, null);
    }

    /** @param ownerId el jugador dueño, o null para un animal sin dueño */
    public Animal registerFounder(LivingEntity entity, Species species, Breed breed, Sex sex,
            GeneticsEngine geneticsEngine, List<Gene> genesForSpecies, UUID ownerId) {

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
        // La etapa sale de la edad (GrowthTask): con edad 0 volvería a ser una cría en el primer ciclo.
        animal.addAge(species.babyStageDurationTicks() + species.juvenileStageDurationTicks());
        animal.setFertility(species.baseFertility() * (breed != null ? breed.fertilityMultiplier() : 1.0));

        animal.setOwnerId(ownerId);
        applyAppearance(entity, breed, GrowthStage.ADULT);
        bindEntity(entity, animal);
        animals.put(animal.id(), animal);
        store.save(animal);

        return animal;
    }

    /** Registra una cría ya nacida (spawneada por {@code PregnancyManager}) con su genética ya resuelta. */
    public void registerNewborn(LivingEntity entity, Animal animal) {
        bindEntity(entity, animal);
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

        if (!isTracked(entity)) {
            return Optional.empty();
        }

        Animal animal = get(animalIdOf(entity)).orElse(null);

        // Una entidad vieja que reaparece después de que el animal se recuperara en otra: no es él.
        return animal != null && animal.entityId().equals(entity.getUniqueId()) ? Optional.of(animal) : Optional.empty();
    }

    /** El id de animal que dice representar la entidad (aunque ya no sea la vigente). */
    public UUID animalIdOf(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(AnimalKeys.ANIMAL_ID, PersistentDataType.STRING);
        try {
            return raw != null ? UUID.fromString(raw) : entity.getUniqueId();
        } catch (IllegalArgumentException e) {
            return entity.getUniqueId();
        }
    }

    /**
     * Aplica (o remueve) el reskin visual propio de la raza — punto centralizado
     * que reemplaza la lógica repetida en cada call-site de spawn. La escala del
     * display se reduce en la etapa BABY, igual que ya hace vanilla vía Ageable.
     */
    public void applyAppearance(LivingEntity entity, Breed breed, GrowthStage stage) {

        // El modelo animado de la raza, en los adultos: manda sobre el reskin.
        if (ModelsIntegration.ensure(entity, modelFor(breed, stage))) {
            EntityReskinService.apply(plugin, entity, EntityReskin.NONE);
            return;
        }

        ModelsIntegration.remove(entity);

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

        // FMM no guarda el modelo con la entidad: tras un reinicio o una recarga de chunk hay que volver a ponerlo.
        String model = modelFor(breed, stage);
        if (model != null) {
            if (ModelsIntegration.ensure(entity, model)) {
                return;
            }
        } else {
            ModelsIntegration.remove(entity);
        }

        EntityReskin reskin = breed != null ? breed.reskin() : EntityReskin.NONE;

        if (!reskin.isActive()) {
            return;
        }

        double scaleFactor = stage == GrowthStage.BABY ? 0.5 : 1.0;
        EntityReskinService.ensureAttached(plugin, entity,
                new EntityReskin(reskin.material(), reskin.customModelData(), reskin.scale() * scaleFactor, reskin.yOffset()));
    }

    private static String modelFor(Breed breed, GrowthStage stage) {
        return breed != null && (stage == GrowthStage.ADULT || stage == GrowthStage.ELDER) ? breed.model() : null;
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
