package com.sack.rpgroll.workers.core.skin;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.sack.rpgroll.common.reskin.EntityReskinService;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerKeys;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import io.papermc.paper.datacomponent.item.ResolvableProfile;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El cuerpo con skin de un worker.
 * <p>
 * Un worker tiene que ser un mob para caminar (se mueve con el pathfinder), y un mob no lleva
 * skin de jugador; el maniquí ({@link Mannequin}, el mismo que usa RPGRoll-NPCs) sí, pero no
 * camina. Así que el mob sigue haciéndolo todo, invisible y callado, y un maniquí con la skin lo
 * sigue a cada tick: se pone en su sitio, mira hacia donde mira él, lleva en la mano la
 * herramienta de su profesión y la balancea mientras trabaja cerca de su objetivo.
 * <p>
 * El maniquí no se guarda con el mundo ({@code persistent = false}): si se descarga el chunk
 * desaparece, y se vuelve a crear en cuanto el mob vuelve a estar cargado. En su PDC lleva el
 * uuid del worker ({@link WorkerKeys#BODY}), así que {@link WorkerManager#resolve} lo trata como
 * el propio worker y mirarlo con {@code /workers} funciona igual.
 */
public class WorkerBodyService {

    private static final int SWING_EVERY_TICKS = 12;
    private static final int SLOW_CHECK_TICKS = 20;
    private static final double WORK_REACH_SQUARED = 3.5 * 3.5;

    private final WorkerManager workerManager;
    private final ProfessionManager professionManager;

    /** worker -> el maniquí que le hace de cuerpo */
    private final Map<UUID, UUID> bodies = new HashMap<>();
    /** worker -> el nombre que lleva su maniquí, para no reescribirlo a cada tick */
    private final Map<UUID, String> names = new HashMap<>();
    private long tick;

    public WorkerBodyService(WorkerManager workerManager, ProfessionManager professionManager) {
        this.workerManager = workerManager;
        this.professionManager = professionManager;
    }

    /** Una pasada por tick: crea, mueve o retira los cuerpos. */
    public void tick() {

        tick++;
        boolean slow = tick % SLOW_CHECK_TICKS == 0;

        for (Worker worker : workerManager.getAll()) {

            if (!worker.hasSkin()) {
                if (slow) {
                    revealIfHidden(worker);
                }
                continue;
            }

            LivingEntity driver = driver(worker.id());

            if (driver == null) {
                discard(worker.id());
                continue;
            }

            Mannequin body = body(worker.id());

            if (body == null) {
                body = spawn(worker, driver);
            }

            follow(worker, driver, body, slow);
        }

        if (slow) {
            for (UUID workerId : List.copyOf(bodies.keySet())) {
                if (!workerManager.get(workerId).map(Worker::hasSkin).orElse(false)) {
                    discard(workerId);
                }
            }
        }
    }

    /** La skin del worker acaba de cambiar (o de quitarse): se aplica ya, sin esperar al tick. */
    public void refresh(Worker worker) {

        discard(worker.id());
        LivingEntity driver = driver(worker.id());

        if (driver == null) {
            return;
        }

        if (worker.hasSkin()) {
            spawn(worker, driver);
        } else {
            reveal(driver, worker);
        }
    }

    /** Al apagar: fuera los cuerpos y los mobs otra vez a la vista (si se quita el plugin, no quedan invisibles). */
    public void removeAll() {

        for (UUID workerId : List.copyOf(bodies.keySet())) {

            discard(workerId);
            LivingEntity driver = driver(workerId);

            if (driver != null) {
                reveal(driver, null);
            }
        }
    }

    /** Maniquíes de una sesión anterior que se quedaron en el mundo (un /reload a medias, un cierre en seco). */
    public void purgeOrphans() {

        for (World world : Bukkit.getWorlds()) {
            for (Mannequin mannequin : world.getEntitiesByClass(Mannequin.class)) {
                if (isBody(mannequin)) {
                    mannequin.remove();
                }
            }
        }
    }

    public boolean isBody(Entity entity) {
        return entity instanceof Mannequin
                && entity.getPersistentDataContainer().has(WorkerKeys.BODY, PersistentDataType.STRING);
    }

    /** El mob de un worker a partir de su maniquí, o null si {@code entity} no es un cuerpo. */
    public LivingEntity driverOf(Entity entity) {

        if (!isBody(entity)) {
            return null;
        }

        try {
            return driver(UUID.fromString(entity.getPersistentDataContainer().get(WorkerKeys.BODY,
                    PersistentDataType.STRING)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** El mob recibió un golpe: el cuerpo lo acusa (el mob está callado, así que suena aquí). */
    public void hurt(LivingEntity driver) {

        Mannequin body = body(driver.getUniqueId());

        if (body != null) {
            body.playHurtAnimation(0);
            body.getWorld().playSound(body.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);
        }
    }

    /** El mob murió: el cuerpo se deshace en humo. */
    public void died(LivingEntity driver) {

        Mannequin body = body(driver.getUniqueId());

        if (body != null) {
            body.getWorld().spawnParticle(Particle.POOF, body.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        }

        discard(driver.getUniqueId());
    }

    private Mannequin spawn(Worker worker, LivingEntity driver) {

        Profession profession = professionManager.get(worker.professionId()).orElse(null);
        String name = nameOf(worker, profession);
        Material tool = toolOf(profession);
        ResolvableProfile profile = profileOf(worker.skin());

        Mannequin body = driver.getWorld().spawn(driver.getLocation(), Mannequin.class, mannequin -> {
            mannequin.setPersistent(false);
            mannequin.getPersistentDataContainer().set(WorkerKeys.BODY, PersistentDataType.STRING,
                    worker.id().toString());
            mannequin.setProfile(profile);
            mannequin.setDescription(null);
            mannequin.setImmovable(true);
            mannequin.setInvulnerable(true);
            mannequin.setSilent(true);
            mannequin.setGravity(false);
            mannequin.setCollidable(false);
            mannequin.setRemoveWhenFarAway(false);
            mannequin.customName(ComponentUtils.parse(name));
            mannequin.setCustomNameVisible(true);
            mannequin.setBodyYaw(driver.getBodyYaw());

            if (tool != null) {
                mannequin.getEquipment().setItemInMainHand(new ItemStack(tool));
            }
        });

        bodies.put(worker.id(), body.getUniqueId());
        names.put(worker.id(), name);
        hide(driver);

        return body;
    }

    private void follow(Worker worker, LivingEntity driver, Mannequin body, boolean slow) {

        Location to = driver.getLocation();
        Location at = body.getLocation();

        if (at.getWorld() != to.getWorld() || at.distanceSquared(to) > 1.0E-4
                || Math.abs(at.getYaw() - to.getYaw()) > 0.5f || Math.abs(at.getPitch() - to.getPitch()) > 0.5f) {
            body.teleport(to);
        }

        body.setBodyYaw(driver.getBodyYaw());

        if (worker.currentAction() == AiAction.WORK
                && Math.floorMod(tick + worker.id().hashCode(), SWING_EVERY_TICKS) == 0) {

            Location target = worker.currentTarget();

            if (target != null && target.getWorld() == to.getWorld() && target.distanceSquared(to) <= WORK_REACH_SQUARED) {
                body.swingMainHand();
            }
        }

        if (slow) {

            String name = nameOf(worker, professionManager.get(worker.professionId()).orElse(null));

            if (!name.equals(names.get(worker.id()))) {
                body.customName(ComponentUtils.parse(name));
                names.put(worker.id(), name);
            }
        }
    }

    private void hide(LivingEntity driver) {

        // El modelo de profesión (un ItemDisplay montado) flotaría encima de la skin.
        EntityReskinService.remove(driver);
        PersistentDataContainer pdc = driver.getPersistentDataContainer();

        if (pdc.has(WorkerKeys.HIDDEN, PersistentDataType.BOOLEAN)) {
            return;
        }

        pdc.set(WorkerKeys.HIDDEN, PersistentDataType.BOOLEAN, true);
        driver.setInvisible(true);
        driver.setSilent(true);
    }

    /** Vuelve a enseñar el mob; con {@code worker}, le devuelve también el aspecto de su profesión. */
    private void reveal(LivingEntity driver, Worker worker) {

        PersistentDataContainer pdc = driver.getPersistentDataContainer();

        if (pdc.has(WorkerKeys.HIDDEN, PersistentDataType.BOOLEAN)) {
            pdc.remove(WorkerKeys.HIDDEN);
            driver.setInvisible(false);
            driver.setSilent(false);
        }

        if (worker != null) {
            professionManager.get(worker.professionId())
                    .ifPresent(profession -> workerManager.applyAppearance(driver, profession));
        }
    }

    /** Se le quitó la skin con el mob descargado: al volver a cargarse sigue escondido y sin cuerpo. */
    private void revealIfHidden(Worker worker) {

        LivingEntity driver = driver(worker.id());

        if (driver != null && driver.getPersistentDataContainer().has(WorkerKeys.HIDDEN, PersistentDataType.BOOLEAN)) {
            reveal(driver, worker);
        }
    }

    private void discard(UUID workerId) {

        UUID bodyId = bodies.remove(workerId);
        names.remove(workerId);

        if (bodyId != null) {
            Entity entity = Bukkit.getEntity(bodyId);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private Mannequin body(UUID workerId) {

        UUID bodyId = bodies.get(workerId);

        if (bodyId == null) {
            return null;
        }

        return Bukkit.getEntity(bodyId) instanceof Mannequin mannequin && mannequin.isValid() ? mannequin : null;
    }

    private static LivingEntity driver(UUID workerId) {
        return Bukkit.getEntity(workerId) instanceof LivingEntity living && living.isValid() ? living : null;
    }

    private static String nameOf(Worker worker, Profession profession) {

        if (worker.customName() != null && !worker.customName().isBlank()) {
            return worker.customName();
        }

        return profession != null ? profession.displayName() : worker.professionId();
    }

    private static Material toolOf(Profession profession) {

        if (profession == null || profession.toolMaterial() == null) {
            return null;
        }

        Material material = Material.matchMaterial(profession.toolMaterial());
        return material != null && material.isItem() ? material : null;
    }

    private static ResolvableProfile profileOf(SkinTexture skin) {

        ProfileProperty textures = skin.signature() != null && !skin.signature().isBlank()
                ? new ProfileProperty("textures", skin.value(), skin.signature())
                : new ProfileProperty("textures", skin.value());

        return ResolvableProfile.resolvableProfile().addProperty(textures).build();
    }

}
