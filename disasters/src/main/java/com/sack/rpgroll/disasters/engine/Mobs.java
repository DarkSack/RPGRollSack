package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.integration.MobsIntegration;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * Mientras dura el desastre aparecen mobs alrededor de los jugadores: strays en la ventisca, husks
 * en la tormenta de arena... Pueden ser vanilla ({@code STRAY}) o de RPGRoll-Mobs
 * ({@code rpgroll:<id>}), con más vida, daño y velocidad según el nivel, equipo, efectos y nombre.
 * Por defecto se van al terminar.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Mobs extends Phenomenon {

    private final List<LivingEntity> spawned = new ArrayList<>();
    private List<String> pool;

    Mobs(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 20);
    }

    @Override
    void tick() {

        spawned.removeIf(entity -> !entity.isValid());
        int interval = (int) Math.max(period, number("interval", 100));
        if (age % interval >= period) {
            return;
        }

        int max = (int) Math.round(scaled("max", 24));
        int perPlayer = (int) Math.round(scaled("per-player", 3));
        double chance = Math.min(1, strength("chance", 0.5));

        for (Player player : engine.targets(world)) {
            if (spawned.size() >= max) {
                return;
            }
            long near = spawned.stream().filter(entity -> entity.getWorld().equals(world)
                    && entity.getLocation().distanceSquared(player.getLocation()) < 48 * 48).count();
            if (near < perPlayer && random() < chance) {
                spawnNear(player.getLocation());
            }
        }
    }

    private void spawnNear(Location near) {

        String entry = pick(pool());
        if (entry == null) {
            return;
        }

        Location at = spot(near);
        if (at == null) {
            return;
        }

        LivingEntity mob;
        if (entry.toLowerCase(Locale.ROOT).startsWith("rpgroll:")) {
            mob = MobsIntegration.spawnMob(entry.substring("rpgroll:".length()), at).orElse(null);
            if (mob == null) {
                engine.warn(params.type() + " 'entities': " + entry + " no salió (¿está RPGRoll-Mobs y existe ese id?)");
                return;
            }
        } else {
            EntityType type;
            try {
                type = EntityType.valueOf(entry.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                engine.warn(params.type() + " 'entities': " + entry + " no es un mob; se omite");
                return;
            }
            if (!type.isAlive() || !type.isSpawnable() || !(world.spawnEntity(at, type) instanceof LivingEntity living)) {
                engine.warn(params.type() + " 'entities': " + entry + " no es un ser vivo; se omite");
                return;
            }
            mob = living;
            dress(mob);
        }

        mob.getPersistentDataContainer().set(engine.tag(), PersistentDataType.BYTE, (byte) 2);
        if (flag("remove-at-end", true)) {
            mob.setPersistent(false);
            mob.setRemoveWhenFarAway(true);
        }
        spawned.add(mob);

        for (Looks.ParticleLook look : particles("spawn-particles", List.of("LARGE_SMOKE*12"))) {
            look.spawn(world, at.clone().add(0, 1, 0), 12, 0.3, 0.6, 0.3, 0.02);
        }
        sound("spawn-sound", "").play(world, at);
    }

    /** Vida, daño y velocidad (×intensidad del nivel), equipo, efectos, brillo y nombre; solo a los vanilla. */
    private void dress(LivingEntity mob) {

        multiply(mob, Attribute.MAX_HEALTH, strength("health", 1));
        multiply(mob, Attribute.ATTACK_DAMAGE, strength("damage", 1));
        multiply(mob, Attribute.MOVEMENT_SPEED, number("speed", 1));
        AttributeInstance health = mob.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            mob.setHealth(health.getValue());
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment != null) {
            for (String spec : params.list("equipment", List.of())) {
                String[] parts = spec.trim().split("\\s+");
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(parts[0].toUpperCase(Locale.ROOT));
                    equipment.setItem(slot, new ItemStack(Looks.item(parts[1])));
                    equipment.setDropChance(slot, 0f);
                } catch (RuntimeException e) {
                    engine.warn(params.type() + " 'equipment': " + spec + " no vale (RANURA OBJETO); se omite");
                }
            }
        }

        for (PotionEffect effect : potions()) {
            mob.addPotionEffect(effect);
        }
        mob.setGlowing(flag("glowing", false));
        String name = params.text("name", "");
        if (!name.isBlank()) {
            mob.customName(ComponentUtils.parse(name));
            mob.setCustomNameVisible(true);
        }
    }

    private static void multiply(LivingEntity mob, Attribute attribute, double factor) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && factor > 0 && factor != 1) {
            instance.setBaseValue(instance.getBaseValue() * factor);
        }
    }

    private List<PotionEffect> potions() {
        List<PotionEffect> effects = new ArrayList<>();
        for (String spec : params.list("mob-effects", List.of())) {
            try {
                effects.add(Looks.potion(spec));
            } catch (RuntimeException e) {
                engine.warn(params.type() + " 'mob-effects': " + spec + " no vale (" + e.getMessage() + "); se omite");
            }
        }
        return effects;
    }

    /**
     * Un sitio en el suelo entre {@code distance-min} y {@code -max} del jugador con hueco para un
     * mob, fuera de la zona segura y, con {@code only-open-sky}, a cielo abierto.
     */
    private Location spot(Location near) {

        double min = number("distance-min", 10);
        double spread = Math.max(0, number("distance-max", 24) - min);
        boolean openSky = flag("only-open-sky", true);

        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random() * Math.PI * 2;
            double distance = min + random() * spread;
            int x = near.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = near.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            Block feet;
            if (openSky) {
                feet = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES).getRelative(BlockFace.UP);
            } else {
                feet = world.getBlockAt(x, near.getBlockY(), z);
                for (int i = 0; i < 6 && !feet.getRelative(BlockFace.DOWN).getType().isSolid(); i++) {
                    feet = feet.getRelative(BlockFace.DOWN);
                }
            }
            Location at = feet.getLocation().add(0.5, 0, 0.5);
            if (feet.isPassable() && !feet.isLiquid() && feet.getRelative(BlockFace.UP).isPassable()
                    && feet.getRelative(BlockFace.DOWN).getType().isSolid() && !engine.inSafeZone(at)) {
                return at;
            }
        }
        return null;
    }

    /** {@code entities} con pesos: {@code ["STRAY*3", "SKELETON"]} saca strays 3 de cada 4 veces. */
    private List<String> pool() {
        if (pool == null) {
            List<String> all = new ArrayList<>();
            for (String raw : params.list("entities", List.of("ZOMBIE"))) {
                Matcher weighted = Looks.WEIGHT.matcher(raw.trim());
                int weight = weighted.matches() ? Math.clamp(Integer.parseInt(weighted.group(2)), 1, 1000) : 1;
                all.addAll(Collections.nCopies(weight, weighted.matches() ? weighted.group(1) : raw.trim()));
            }
            pool = List.copyOf(all);
        }
        return pool;
    }

    @Override
    void cleanup() {
        if (flag("remove-at-end", true)) {
            for (Entity entity : spawned) {
                if (entity.isValid()) {
                    entity.remove();
                }
            }
        }
        spawned.clear();
        super.cleanup();
    }

}
