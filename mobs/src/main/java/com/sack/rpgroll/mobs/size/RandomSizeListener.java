package com.sack.rpgroll.mobs.size;

import com.sack.rpgroll.mobs.api.MobSpawnEvent;
import com.sack.rpgroll.mobs.core.MobCategory;
import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.size.RandomSizeSettings.Category;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Tamaño aleatorio de los mobs al aparecer (atributo {@code scale}): los
 * vanilla y los de RPGRoll.
 * <p>
 * Todo va como modificadores con la clave {@code rpgroll-mobs:random-size},
 * que el juego guarda con la entidad: el tamaño sobrevive a reinicios y no se
 * vuelve a tirar al recargar el chunk. El tamaño elegido queda además en el
 * PDC para escalar la experiencia y el botín al morir.
 * <p>
 * Un mob vanilla con nombre o una mascota no cambia. Los de RPGRoll aparecen
 * con motivo CUSTOM y se tratan aparte, en {@link MobSpawnEvent}, cuando el
 * motor ya les puso su modelo y sus stats: el tamaño se multiplica sobre su
 * escala propia. Su daño no es el atributo sino su stat {@code damage}, así
 * que el factor de daño se guarda en {@link #DAMAGE_KEY} para el ataque.
 */
public class RandomSizeListener implements Listener {

    /** Neutrales: atacan solo si se les provoca. El resto de no hostiles cuenta como pasivo. */
    private static final Set<EntityType> NEUTRAL = Set.of(
            EntityType.WOLF, EntityType.BEE, EntityType.IRON_GOLEM, EntityType.LLAMA, EntityType.TRADER_LLAMA,
            EntityType.PANDA, EntityType.POLAR_BEAR, EntityType.DOLPHIN, EntityType.GOAT);

    /** PDC: cuánto multiplica el tamaño el ataque propio de un mob de RPGRoll. */
    public static final String DAMAGE_KEY = "random-size-damage";

    private final NamespacedKey key;
    private final NamespacedKey damageKey;
    private final NamespacedKey mobDefinitionKey;
    private final Supplier<RandomSizeSettings> settings;

    public RandomSizeListener(Plugin plugin, Supplier<RandomSizeSettings> settings) {
        this.key = new NamespacedKey(plugin, "random-size");
        this.damageKey = new NamespacedKey(plugin, DAMAGE_KEY);
        this.mobDefinitionKey = new NamespacedKey(plugin, "mob-definition-id");
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {

        RandomSizeSettings config = settings.get();
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Mob mob)
                || !config.appliesTo(entity.getWorld().getName(), event.getSpawnReason().name(),
                        entity.getType().getKey().getKey())
                || entity.customName() != null
                || (entity instanceof Tameable tameable && tameable.isTamed())
                || entity.getPersistentDataContainer().has(mobDefinitionKey)
                || entity.getPersistentDataContainer().has(key)) {
            return;
        }

        double scale = config.pick(categoryOf(mob), entity.getType().getKey().getKey(), entity.getHeight(),
                ThreadLocalRandom.current());

        if (Math.abs(scale - 1) < 0.01) {
            return;
        }

        apply(entity, scale, config);
    }

    @EventHandler
    public void onRpgRollSpawn(MobSpawnEvent event) {

        RandomSizeSettings config = settings.get();
        LivingEntity entity = event.getEntity();
        MobDefinition definition = event.getDefinition();

        if (!config.rpgrollMobs() || !config.enabledIn(entity.getWorld().getName())
                || definition.isBoss()
                || "false".equalsIgnoreCase(definition.customData().getOrDefault("random-size", "true"))
                || (!config.rpgrollModeled() && definition.model().modelEngineId() != null
                        && !definition.model().modelEngineId().isBlank())
                || entity.getPersistentDataContainer().has(key)) {
            return;
        }

        Category category = definition.category() == MobCategory.NEUTRAL ? Category.NEUTRAL
                : entity instanceof Enemy ? Category.HOSTILE : Category.PASSIVE;
        double scale = config.pick(category, "rpgroll:" + definition.id(), entity.getHeight(),
                ThreadLocalRandom.current());

        if (Math.abs(scale - 1) < 0.01) {
            return;
        }

        apply(entity, scale, config);
        entity.getPersistentDataContainer().set(damageKey, PersistentDataType.DOUBLE,
                RandomSizeSettings.factor(scale, config.damageWeight()));
    }

    /** Pone el tamaño y lo que lo sigue. */
    private void apply(LivingEntity entity, double scale, RandomSizeSettings config) {

        modify(entity, Attribute.SCALE, scale - 1);
        modify(entity, Attribute.MAX_HEALTH, RandomSizeSettings.factor(scale, config.healthWeight()) - 1);
        modify(entity, Attribute.ATTACK_DAMAGE, RandomSizeSettings.factor(scale, config.damageWeight()) - 1);
        modify(entity, Attribute.MOVEMENT_SPEED, RandomSizeSettings.factor(scale, config.speedWeight()) - 1);

        AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            entity.setHealth(health.getValue());
        }

        entity.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, scale);
    }

    private void modify(LivingEntity entity, Attribute attribute, double amount) {

        AttributeInstance instance = entity.getAttribute(attribute);

        if (instance == null) {
            return;
        }

        instance.removeModifier(key);

        if (Math.abs(amount) > 1.0E-4) {
            instance.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {

        Double scale = event.getEntity().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);

        if (scale == null) {
            return;
        }

        RandomSizeSettings config = settings.get();
        double experience = RandomSizeSettings.factor(scale, config.experienceWeight());
        event.setDroppedExp(randomRound(event.getDroppedExp() * experience));

        if (config.lootWeight() == 0) {
            return;
        }

        double loot = RandomSizeSettings.factor(scale, config.lootWeight());

        for (ItemStack drop : event.getDrops()) {
            // El equipo puesto no se multiplica: solo lo que suelta el mob por ser lo que es.
            if (drop.getMaxStackSize() > 1) {
                drop.setAmount(Math.max(1, Math.min(drop.getMaxStackSize(), randomRound(drop.getAmount() * loot))));
            }
        }
    }

    static Category categoryOf(Mob mob) {

        if (mob instanceof Enemy) {
            return Category.HOSTILE;
        }

        return NEUTRAL.contains(mob.getType()) ? Category.NEUTRAL : Category.PASSIVE;
    }

    /** 2,3 sale 2 el 70 % de las veces y 3 el 30 %: de media, lo justo. */
    private static int randomRound(double value) {
        int whole = (int) Math.floor(value);
        return whole + (ThreadLocalRandom.current().nextDouble() < value - whole ? 1 : 0);
    }

}
