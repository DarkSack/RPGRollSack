package com.sack.rpgroll.ranching.core.ownership;

import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Llamar a un animal (o mandarlo al corral): si su entidad está cargada se mueve; si no, se carga el
 * chunk donde se le vio por última vez y se espera a que aparezca. Si aun así no está —la borró un
 * comando, un plugin de limpieza, un chunk regenerado— se crea una entidad nueva con todos sus datos:
 * genética, edad, salud y dueño viven en el animal, no en la entidad.
 */
public class AnimalRecall {

    public enum Outcome {
        /** Estaba y se movió. */
        MOVED,
        /** No se encontró su entidad y se creó una nueva. */
        RECOVERED,
        /** No se encontró y no se pudo (o no se quiso) recrear. */
        NOT_FOUND,
        /** Ya había una llamada en curso para este animal. */
        BUSY
    }

    /** Lo que se espera a que las entidades de un chunk recién cargado aparezcan. */
    private static final int WAIT_TICKS = 40;

    private final Plugin plugin;
    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final Set<UUID> inFlight = new HashSet<>();
    private boolean recreateMissing = true;

    public AnimalRecall(Plugin plugin, AnimalManager animalManager, SpeciesManager speciesManager,
            BreedManager breedManager) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
    }

    public void configure(boolean recreateMissing) {
        this.recreateMissing = recreateMissing;
    }

    public void bring(Animal animal, Location destination, Consumer<Outcome> done) {

        Entity loaded = animalManager.entityOf(animal).orElse(null);

        if (loaded != null) {
            move(loaded, animal, destination);
            done.accept(Outcome.MOVED);
            return;
        }

        if (!inFlight.add(animal.id())) {
            done.accept(Outcome.BUSY);
            return;
        }

        Animal.LastSeen seen = animal.lastSeen();
        World world = seen == null ? null : Bukkit.getWorld(seen.world());

        if (world == null) {
            finishMissing(animal, destination, done);
            return;
        }

        int chunkX = (int) Math.floor(seen.x()) >> 4;
        int chunkZ = (int) Math.floor(seen.z()) >> 4;

        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {

            chunk.addPluginChunkTicket(plugin);

            new BukkitRunnable() {

                private int waited;

                @Override
                public void run() {

                    Entity entity = animalManager.entityOf(animal).orElse(null);

                    if (entity == null && waited++ < WAIT_TICKS) {
                        return;
                    }

                    cancel();
                    chunk.removePluginChunkTicket(plugin);

                    if (entity != null) {
                        inFlight.remove(animal.id());
                        move(entity, animal, destination);
                        done.accept(Outcome.MOVED);
                    } else {
                        finishMissing(animal, destination, done);
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        });
    }

    private void finishMissing(Animal animal, Location destination, Consumer<Outcome> done) {

        inFlight.remove(animal.id());

        if (!recreateMissing || recreate(animal, destination) == null) {
            done.accept(Outcome.NOT_FOUND);
            return;
        }

        done.accept(Outcome.RECOVERED);
    }

    private void move(Entity entity, Animal animal, Location destination) {

        if (entity instanceof LivingEntity living && living.isLeashed()) {
            living.setLeashHolder(null);
        }

        entity.leaveVehicle();

        // Con el reskin montado encima la entidad no se deja teletransportar: se quita y se vuelve a poner.
        if (entity instanceof LivingEntity living) {
            EntityReskinService.remove(living);
        }

        entity.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        animalManager.updateLastSeen(animal, destination);

        if (entity instanceof LivingEntity living) {
            animalManager.applyAppearance(living, breedOf(animal), animal.stage());
        }
    }

    /** Una entidad nueva para un animal cuya entidad ya no existe. */
    public LivingEntity recreate(Animal animal, Location location) {

        Species species = speciesManager.get(animal.speciesId()).orElse(null);

        if (species == null || location.getWorld() == null) {
            return null;
        }

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location,
                animalManager.resolveEntityType(species));
        entity.setRemoveWhenFarAway(false);

        if (entity instanceof Ageable ageable) {
            if (animal.stage() == GrowthStage.BABY || animal.stage() == GrowthStage.JUVENILE) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }

        animalManager.bindEntity(entity, animal);
        animalManager.applyAppearance(entity, breedOf(animal), animal.stage());
        animalManager.save(animal);

        return entity;
    }

    private Breed breedOf(Animal animal) {
        return animal.breedId() == null ? null : breedManager.get(animal.breedId()).orElse(null);
    }

}
