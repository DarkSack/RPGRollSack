package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.traps.condition.TrapConditionContext;
import com.sack.rpgroll.traps.condition.TrapConditionEvaluator;
import com.sack.rpgroll.traps.integration.MobsIntegration;
import com.sack.rpgroll.traps.core.TrapAction;
import com.sack.rpgroll.traps.registry.TrapActionContext;
import com.sack.rpgroll.traps.registry.TrapActionRegistry;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Escanea objetivos y dispara para cada {@link PlacedTurret} — tarea propia,
 * separada de {@code TrapEngine.tick()} (que asume siempre un Player como
 * disparador de un evento puntual; una torreta reevalúa objetivo cada tick
 * de forma continua, jugador o mob).
 */
public class TurretEngine {

    private final JavaPlugin plugin;
    private final TurretManager turretManager;
    private final PlacedTurretManager placedTurretManager;
    private final TrapConditionEvaluator conditionEvaluator;
    private final TrapActionRegistry actionRegistry;
    private final com.sack.rpgroll.traps.ammo.AmmoManager ammoManager;
    private final NamespacedKey placementKey;

    private final Map<String, TurretRuntimeState> runtime = new HashMap<>();
    private BukkitTask task;

    public TurretEngine(JavaPlugin plugin, TurretManager turretManager, PlacedTurretManager placedTurretManager,
            TrapConditionEvaluator conditionEvaluator, TrapActionRegistry actionRegistry, com.sack.rpgroll.traps.ammo.AmmoManager ammoManager) {
        this.plugin = plugin;
        this.turretManager = turretManager;
        this.placedTurretManager = placedTurretManager;
        this.conditionEvaluator = conditionEvaluator;
        this.actionRegistry = actionRegistry;
        this.ammoManager = ammoManager;
        this.placementKey = new NamespacedKey(plugin, "turret-placement");
    }

    public void start(long intervalTicks) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {

        for (PlacedTurret placed : List.copyOf(placedTurretManager.getAll())) {

            Optional<TurretDefinition> definitionOpt = turretManager.get(placed.turretId());
            if (definitionOpt.isEmpty()) {
                continue;
            }

            TurretDefinition definition = definitionOpt.get();
            Location anchor = placed.location();

            if (anchor == null || anchor.getWorld() == null) {
                continue;
            }

            TurretRuntimeState state = runtime.computeIfAbsent(placed.placementId(), id -> new TurretRuntimeState());
            ensureBaseBlock(anchor, definition);
            ItemDisplay visual = ensureVisual(placed, definition, state, anchor);

            LivingEntity target = findTarget(anchor, definition, placed);

            if (target == null) {
                state.targetEntity = null;

                // Sin objetivo gira sobre sí misma: se ve a simple vista que
                // está buscando y no apagada.
                if (definition.spinDegreesPerTick() > 0) {
                    state.idleYaw = (float) ((state.idleYaw + definition.spinDegreesPerTick()) % 360.0);
                    visual.setRotation(state.idleYaw, 0);
                }

                continue;
            }

            state.targetEntity = target.getUniqueId();

            Location eye = hoverLocation(anchor, definition);
            Vector direction = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector()
                    .subtract(eye.toVector()).normalize();

            visual.setRotation(eye.clone().setDirection(direction).getYaw(), 0);

            long now = System.currentTimeMillis();

            if (now >= state.nextFireAtMillis && fire(placed, eye, target, definition)) {
                state.nextFireAtMillis = now + definition.fireIntervalTicks() * 50L;
            }
        }
    }

    private LivingEntity findTarget(Location anchor, TurretDefinition definition, PlacedTurret placed) {

        World world = anchor.getWorld();
        double radiusSquared = definition.radius() * definition.radius();

        LivingEntity best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (LivingEntity candidate : world.getNearbyLivingEntities(anchor, definition.radius())) {

            if (!isValidTarget(candidate, definition, placed)) {
                continue;
            }

            double distanceSquared = candidate.getLocation().distanceSquared(anchor);

            if (distanceSquared <= radiusSquared && distanceSquared < bestDistanceSquared) {
                best = candidate;
                bestDistanceSquared = distanceSquared;
            }
        }

        return best;
    }

    /**
     * El filtro de objetivos combina la definición (qué puede hacer esta
     * torreta) con la configuración de la instancia (qué quiere su dueño).
     */
    private boolean isValidTarget(LivingEntity candidate, TurretDefinition definition, PlacedTurret placed) {

        TurretTargeting targeting = placed.targeting();

        if (candidate instanceof Player player) {

            if (!definition.targetPlayers()) {
                return false;
            }

            // Creativo y espectador quedan fuera: una torreta no debe
            // dispararle a alguien que está construyendo.
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                return false;
            }

            boolean ally = TurretAccess.isAlly(player, placed);

            if (ally ? !targeting.allies() : !targeting.enemies()) {
                return false;
            }

            return conditionEvaluator.evaluateAll(definition.conditions(), new TrapConditionContext(player));
        }

        boolean hostile = candidate instanceof Monster || MobsIntegration.isCustomHostile(candidate);

        if (hostile) {
            return definition.targetHostileMobs() && targeting.hostileMobs();
        }

        return targeting.passiveMobs();
    }

    /**
     * Consume una unidad de munición y ejecuta su impacto contra el objetivo.
     * <p>
     * La munición decide QUÉ pasa al impactar y la torreta la puntería, así
     * que una misma torreta cambia de comportamiento según lo que se le
     * cargue. Si la munición no define impacto propio, se usa el de la
     * torreta.
     *
     * @return false si no había munición — la torreta no dispara.
     */
    private boolean fire(PlacedTurret placed, Location origin, LivingEntity target, TurretDefinition definition) {

        String ammoId = placed.ammo().entrySet().stream()
                .filter(slot -> slot.getValue() > 0)
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (ammoId == null) {
            return false;
        }

        java.util.Map<String, Integer> remaining = new java.util.HashMap<>(placed.ammo());
        int left = remaining.get(ammoId) - 1;

        if (left > 0) {
            remaining.put(ammoId, left);
        } else {
            remaining.remove(ammoId);
        }

        placedTurretManager.setAmmo(placed.placementId(), remaining);

        TrapAction impact = ammoManager.get(ammoId)
                .map(com.sack.rpgroll.traps.ammo.AmmoDefinition::impact)
                .orElse(null);

        TrapActionContext context = new TrapActionContext(null, null, target, null, origin,
                "turret:" + placed.turretId());

        actionRegistry.execute(impact != null ? impact : definition.impact(), context);
        return true;
    }

    private ItemDisplay ensureVisual(PlacedTurret placed, TurretDefinition definition, TurretRuntimeState state,
            Location anchor) {

        if (state.visualEntity != null) {
            Entity cached = Bukkit.getEntity(state.visualEntity);
            if (cached instanceof ItemDisplay display && display.isValid()) {
                return display;
            }
        }

        for (Entity nearby : anchor.getWorld().getNearbyEntities(anchor, 1, 1, 1)) {
            if (nearby instanceof ItemDisplay display
                    && placed.placementId().equals(
                            display.getPersistentDataContainer().get(placementKey, PersistentDataType.STRING))) {
                state.visualEntity = display.getUniqueId();
                return display;
            }
        }

        ItemDisplay display = anchor.getWorld().spawn(hoverLocation(anchor, definition), ItemDisplay.class);
        display.getPersistentDataContainer().set(placementKey, PersistentDataType.STRING, placed.placementId());
        display.setBillboard(Display.Billboard.FIXED);
        display.setItemStack(buildModel(definition));

        state.visualEntity = display.getUniqueId();
        return display;
    }

    /** Posición del ítem flotante: centrado sobre el bloque base. */
    private Location hoverLocation(Location anchor, TurretDefinition definition) {
        return anchor.clone().add(0.5, definition.hoverHeight(), 0.5);
    }

    /**
     * Coloca el bloque base si la definición lo pide y todavía no está.
     * <p>
     * Se comprueba el material actual antes de escribir para no ensuciar el
     * mundo en cada tick ni pisar un bloque que el admin haya cambiado a mano.
     */
    private void ensureBaseBlock(Location anchor, TurretDefinition definition) {

        Material base = definition.baseBlock();

        if (base == null) {
            return;
        }

        if (anchor.getBlock().getType() != base) {
            anchor.getBlock().setType(base);
        }
    }

    private ItemStack buildModel(TurretDefinition definition) {

        ItemStack item = new ItemStack(definition.model());

        if (definition.customModelData() != null) {
            var meta = item.getItemMeta();
            meta.setCustomModelData(definition.customModelData());
            item.setItemMeta(meta);
        }

        return item;
    }

    /** Elimina la entidad visual de una torreta — llamado al remover una instancia colocada. */
    public void despawnVisual(String placementId) {

        TurretRuntimeState state = runtime.remove(placementId);

        if (state != null && state.visualEntity != null) {
            Entity entity = Bukkit.getEntity(state.visualEntity);
            if (entity != null) {
                entity.remove();
            }
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (placementId.equals(
                        entity.getPersistentDataContainer().get(placementKey, PersistentDataType.STRING))) {
                    entity.remove();
                }
            }
        }
    }

}
