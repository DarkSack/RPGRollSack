package com.sack.rpgroll.workers.core.worker;

import com.sack.rpgroll.workers.core.profession.Profession;

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

/** Registro en memoria de todos los workers rastreados — mismo criterio que {@code AnimalManager} de RPGRoll-Ranching. */
public class WorkerManager {

    private static final PersonalityTrait[] PERSONALITIES = PersonalityTrait.values();

    private final Plugin plugin;
    private final WorkerStore store;
    private final Map<UUID, Worker> workers = new HashMap<>();
    private final Random random = new Random();

    public WorkerManager(Plugin plugin) {
        this.plugin = plugin;
        this.store = new WorkerStore(plugin);
    }

    public void loadAll() {

        workers.clear();

        for (Worker worker : store.loadAll()) {
            workers.put(worker.id(), worker);
        }

        plugin.getLogger().info("✔ Workers cargados: " + workers.size());
    }

    public void saveAll() {
        workers.values().forEach(store::save);
    }

    public void save(Worker worker) {
        store.save(worker);
    }

    public Optional<Worker> get(UUID id) {
        return Optional.ofNullable(workers.get(id));
    }

    public Collection<Worker> getAll() {
        return workers.values();
    }

    public List<Worker> getByProfession(String professionId) {
        return workers.values().stream().filter(worker -> worker.professionId().equals(professionId)).toList();
    }

    public void remove(UUID id) {
        workers.remove(id);
        store.delete(id);
    }

    public Worker spawn(LivingEntity entity, Profession profession, PersonalityTrait personality) {

        Worker worker = new Worker(entity.getUniqueId(), profession.id(),
                personality != null ? personality : randomPersonality());

        tagEntity(entity, profession.id());
        workers.put(worker.id(), worker);
        store.save(worker);

        return worker;
    }

    public PersonalityTrait randomPersonality() {
        return PERSONALITIES[random.nextInt(PERSONALITIES.length)];
    }

    public void tagEntity(Entity entity, String professionId) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(WorkerKeys.TRACKED, PersistentDataType.BOOLEAN, true);
        pdc.set(WorkerKeys.PROFESSION_ID, PersistentDataType.STRING, professionId);
    }

    public boolean isTracked(Entity entity) {
        return entity.getPersistentDataContainer().has(WorkerKeys.TRACKED, PersistentDataType.BOOLEAN);
    }

    public Optional<Worker> resolve(Entity entity) {
        return isTracked(entity) ? get(entity.getUniqueId()) : Optional.empty();
    }

    public EntityType resolveEntityType(Profession profession) {

        try {
            return EntityType.valueOf(profession.entityType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ '" + profession.entityType() + "' no es un EntityType válido para la profesión '"
                    + profession.id() + "' — usando VILLAGER.");
            return EntityType.VILLAGER;
        }
    }

}
