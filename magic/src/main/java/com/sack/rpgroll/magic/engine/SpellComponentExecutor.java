package com.sack.rpgroll.magic.engine;

import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.magic.core.SpellComponent;
import com.sack.rpgroll.sackeffects.api.SackEffectsAPI;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ejecuta los componentes "instantáneos" de un {@link
 * com.sack.rpgroll.magic.core.Spell} — todo menos PROJECTILE/DELAY, que
 * {@code SpellCastEngine} maneja directamente porque necesitan pausar el
 * pipeline entre ticks. Un error en un componente solo loguea un warning y
 * no interrumpe el resto, mismo criterio que EffectComponentExecutor/
 * SackEffects EffectEngine.
 */
public class SpellComponentExecutor {

    private final Plugin plugin;

    public SpellComponentExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    public void execute(SpellComponent component, SpellCastContext context) {

        try {
            switch (component.type()) {
                case DASH -> executeDash(component, context);
                case TELEPORT -> executeTeleport(component, context);
                case LEAP -> executeLeap(component, context);
                case ORBIT -> executeOrbit(component, context);
                case DAMAGE_DIRECT -> executeDamageDirect(component, context);
                case DAMAGE_AREA -> executeDamageArea(component, context);
                case DAMAGE_LINE -> executeDamageLine(component, context);
                case DAMAGE_CHAIN -> executeDamageChain(component, context);
                case DAMAGE_CONE -> executeDamageCone(component, context);
                case PARTICLE -> executeParticle(component, context);
                case SOUND -> executeSound(component, context);
                case VISUAL -> executeVisual(component, context);
                case BREAK_BLOCK -> executeBreakBlock(component, context);
                case PLACE_BLOCK -> executePlaceBlock(component, context);
                case IGNITE -> executeIgnite(component, context);
                case FREEZE_WATER -> executeFreezeWater(component, context);
                case SUMMON -> executeSummon(component, context);
                case HEAL -> executeHeal(component, context);
                case APPLY_EFFECT -> executeApplyEffect(component, context);
                case REMOVE_EFFECT -> executeRemoveEffect(component, context);
                case PUSH -> executePush(component, context);
                case PULL -> executePull(component, context);
                case MESSAGE -> executeMessage(component, context);
                case COMMAND -> executeCommand(component, context);
                case PROJECTILE, DELAY -> {
                    // Manejados por SpellCastEngine — necesitan agendar/pausar el pipeline.
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error ejecutando componente " + component.type() + " del hechizo '"
                    + context.spell().id() + "': " + e.getMessage());
        }
    }

    // ============ Movimiento instantáneo ============

    private void executeDash(SpellComponent component, SpellCastContext context) {

        Player caster = context.caster();
        double distance = component.paramDouble("distance", 5) * context.rangeMultiplier();

        Location destination = rayTraceStop(caster.getEyeLocation(), caster.getLocation().getDirection(), distance);
        destination.setDirection(caster.getLocation().getDirection());

        caster.teleport(destination);
        context.setCurrentLocation(destination);
    }

    private void executeTeleport(SpellComponent component, SpellCastContext context) {

        String mode = component.param("mode", "self").toLowerCase(Locale.ROOT);
        Player caster = context.caster();

        if (mode.equals("to_target") && !context.currentTargets().isEmpty()) {
            caster.teleport(context.currentTargets().get(0).getLocation());
        } else {
            Location destination = context.currentLocation().clone();
            destination.setDirection(caster.getLocation().getDirection());
            caster.teleport(destination);
        }
    }

    private void executeLeap(SpellComponent component, SpellCastContext context) {

        double forward = component.paramDouble("forward", 1.5);
        double up = component.paramDouble("up", 0.6);

        Vector direction = context.caster().getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(forward).setY(up);

        context.caster().setVelocity(velocity);
    }

    private void executeOrbit(SpellComponent component, SpellCastContext context) {

        double radius = component.paramDouble("radius", 2);
        int durationTicks = component.paramInt("duration", 40);
        int points = Math.max(4, component.paramInt("points", 16));
        Particle particle = parseParticle(component.param("particle", "WITCH"));
        Location center = context.currentLocation().clone();
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {

                if (elapsed >= durationTicks) {
                    cancel();
                    return;
                }

                double angle = 2 * Math.PI * (elapsed % points) / points;
                Location point = center.clone().add(radius * Math.cos(angle), 0.5, radius * Math.sin(angle));
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0);

                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ============ Daño ============

    private void executeDamageDirect(SpellComponent component, SpellCastContext context) {

        List<LivingEntity> targets = context.currentTargets();

        if (targets.isEmpty()) {
            LivingEntity fallback = rayTraceEntity(context, component.paramDouble("range", 20));
            if (fallback != null) {
                targets = List.of(fallback);
                context.setCurrentTargets(targets);
            }
        }

        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();

        for (LivingEntity target : targets) {
            target.damage(amount, context.caster());
        }

        triggerExplosiveIfPending(context);
    }

    private void executeDamageArea(SpellComponent component, SpellCastContext context) {

        double radius = component.paramDouble("radius", 4) * context.rangeMultiplier();
        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();
        Location center = context.currentLocation();
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        List<LivingEntity> hit = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.equals(context.caster())) {
                living.damage(amount, context.caster());
                hit.add(living);
            }
        }

        context.setCurrentTargets(hit);
        triggerExplosiveIfPending(context);
    }

    private void executeDamageLine(SpellComponent component, SpellCastContext context) {

        double range = component.paramDouble("range", 10) * context.rangeMultiplier();
        double width = component.paramDouble("width", 1.0);
        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();

        Player caster = context.caster();
        Location origin = caster.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        World world = origin.getWorld();

        if (world == null) {
            return;
        }

        List<LivingEntity> hit = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(origin, range, range, range)) {

            if (!(entity instanceof LivingEntity living) || entity.equals(caster)) {
                continue;
            }

            Vector toEntity = entity.getLocation().toVector().subtract(origin.toVector());
            double along = toEntity.dot(direction);

            if (along < 0 || along > range) {
                continue;
            }

            double perpendicular = toEntity.subtract(direction.clone().multiply(along)).length();

            if (perpendicular <= width) {
                living.damage(amount, caster);
                hit.add(living);
            }
        }

        context.setCurrentTargets(hit);
        triggerExplosiveIfPending(context);
    }

    private void executeDamageChain(SpellComponent component, SpellCastContext context) {

        int maxJumps = component.paramInt("max-jumps", 3);
        double radius = component.paramDouble("radius", 6) * context.rangeMultiplier();
        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();

        Player caster = context.caster();
        Location jumpPoint = context.currentLocation();
        World world = jumpPoint.getWorld();

        if (world == null) {
            return;
        }

        List<LivingEntity> hit = new ArrayList<>();

        for (int jump = 0; jump < maxJumps; jump++) {

            LivingEntity nearest = null;
            double nearestDistanceSquared = Double.MAX_VALUE;

            for (Entity entity : world.getNearbyEntities(jumpPoint, radius, radius, radius)) {

                if (!(entity instanceof LivingEntity living) || entity.equals(caster)
                        || context.alreadyHit().contains(entity.getUniqueId())) {
                    continue;
                }

                double distanceSquared = entity.getLocation().distanceSquared(jumpPoint);

                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared;
                    nearest = living;
                }
            }

            if (nearest == null) {
                break;
            }

            nearest.damage(amount, caster);
            hit.add(nearest);
            context.alreadyHit().add(nearest.getUniqueId());
            jumpPoint = nearest.getLocation();
        }

        context.setCurrentTargets(hit);
        triggerExplosiveIfPending(context);
    }

    private void executeDamageCone(SpellComponent component, SpellCastContext context) {

        double range = component.paramDouble("range", 6) * context.rangeMultiplier();
        double angleDegrees = component.paramDouble("angle-degrees", 60);
        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();

        Player caster = context.caster();
        Location origin = caster.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        double cosThreshold = Math.cos(Math.toRadians(angleDegrees / 2.0));
        World world = origin.getWorld();

        if (world == null) {
            return;
        }

        List<LivingEntity> hit = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(origin, range, range, range)) {

            if (!(entity instanceof LivingEntity living) || entity.equals(caster)) {
                continue;
            }

            Vector toEntity = entity.getLocation().toVector().subtract(origin.toVector());
            double distance = toEntity.length();

            if (distance > range || distance < 0.001) {
                continue;
            }

            double cosAngle = toEntity.normalize().dot(direction);

            if (cosAngle >= cosThreshold) {
                living.damage(amount, caster);
                hit.add(living);
            }
        }

        context.setCurrentTargets(hit);
        triggerExplosiveIfPending(context);
    }

    // ============ Visual ============

    private void executeParticle(SpellComponent component, SpellCastContext context) {

        Particle particle = parseParticle(component.param("particle", "FLAME"));
        int count = component.paramInt("count", 10);
        World world = context.currentLocation().getWorld();

        if (world != null) {
            world.spawnParticle(particle, context.currentLocation(), count, 0.3, 0.3, 0.3, 0.01);
        }
    }

    private void executeSound(SpellComponent component, SpellCastContext context) {

        Sound sound = parseSound(component.param("sound", "ENTITY_ILLUSIONER_CAST_SPELL"));
        float volume = (float) component.paramDouble("volume", 1.0);
        float pitch = (float) component.paramDouble("pitch", 1.0);
        World world = context.currentLocation().getWorld();

        if (world != null) {
            world.playSound(context.currentLocation(), sound, volume, pitch);
        }
    }

    private void executeVisual(SpellComponent component, SpellCastContext context) {

        String effectId = component.param("effect-id", null);

        if (effectId == null || !SackEffectsAPI.isReady()) {
            return;
        }

        SackEffectsAPI.get().play(effectId, context.caster(), context.currentLocation());
    }

    // ============ Mundo ============

    private void executeBreakBlock(SpellComponent component, SpellCastContext context) {

        Block block = context.currentLocation().getBlock();

        if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
            block.breakNaturally();
        }
    }

    private void executePlaceBlock(SpellComponent component, SpellCastContext context) {

        Material material = parseMaterial(component.param("material", "STONE"));
        Block block = context.currentLocation().getBlock();

        if (block.getType().isAir()) {
            block.setType(material);
        }
    }

    private void executeIgnite(SpellComponent component, SpellCastContext context) {

        int fireTicks = component.paramInt("fire-ticks", 100);

        if (!context.currentTargets().isEmpty()) {
            for (LivingEntity target : context.currentTargets()) {
                target.setFireTicks(fireTicks);
            }
            return;
        }

        Block block = context.currentLocation().getBlock().getRelative(0, 1, 0);

        if (block.getType().isAir()) {
            block.setType(Material.FIRE);
        }
    }

    private void executeFreezeWater(SpellComponent component, SpellCastContext context) {

        double radius = component.paramDouble("radius", 3) * context.rangeMultiplier();
        Location center = context.currentLocation();
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -r; z <= r; z++) {

                    Location point = center.clone().add(x, y, z);

                    if (point.distanceSquared(center) > radius * radius) {
                        continue;
                    }

                    Block block = point.getBlock();

                    if (block.getType() == Material.WATER) {
                        block.setType(Material.ICE);
                    }
                }
            }
        }
    }

    // ============ Entidades ============

    private void executeSummon(SpellComponent component, SpellCastContext context) {

        EntityType type = parseEntityType(component.param("entity", "ZOMBIE"));
        World world = context.currentLocation().getWorld();

        if (world != null) {
            world.spawnEntity(context.currentLocation(), type);
        }
    }

    private void executeHeal(SpellComponent component, SpellCastContext context) {

        double amount = component.paramDouble("amount", 5) * context.powerMultiplier();
        String target = component.param("target", "self").toLowerCase(Locale.ROOT);

        List<LivingEntity> recipients = target.equals("target") ? context.currentTargets()
                : List.of(context.caster());

        for (LivingEntity recipient : recipients) {

            var maxHealthAttribute = recipient.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : recipient.getHealth();

            recipient.setHealth(Math.min(maxHealth, recipient.getHealth() + amount));
        }
    }

    private void executeApplyEffect(SpellComponent component, SpellCastContext context) {

        String effectId = component.param("effect-id", null);

        if (effectId == null || !EffectsAPI.isReady()) {
            return;
        }

        String target = component.param("target", "target").toLowerCase(Locale.ROOT);
        List<LivingEntity> recipients = target.equals("self") ? List.of(context.caster())
                : context.currentTargets();

        for (LivingEntity recipient : recipients) {
            EffectsAPI.get().apply(effectId, recipient, context.caster());
        }
    }

    private void executeRemoveEffect(SpellComponent component, SpellCastContext context) {

        String effectId = component.param("effect-id", null);

        if (effectId == null || !EffectsAPI.isReady()) {
            return;
        }

        String target = component.param("target", "target").toLowerCase(Locale.ROOT);
        List<LivingEntity> recipients = target.equals("self") ? List.of(context.caster())
                : context.currentTargets();

        for (LivingEntity recipient : recipients) {
            EffectsAPI.get().remove(effectId, recipient);
        }
    }

    private void executePush(SpellComponent component, SpellCastContext context) {

        double strength = component.paramDouble("strength", 1.5);
        Vector direction = context.caster().getLocation().getDirection().normalize();

        for (LivingEntity target : context.currentTargets()) {
            target.setVelocity(direction.clone().multiply(strength).setY(0.3));
        }
    }

    private void executePull(SpellComponent component, SpellCastContext context) {

        double strength = component.paramDouble("strength", 1.5);
        Location casterLocation = context.caster().getLocation();

        for (LivingEntity target : context.currentTargets()) {

            Vector direction = casterLocation.toVector().subtract(target.getLocation().toVector());

            if (direction.lengthSquared() < 0.01) {
                continue;
            }

            target.setVelocity(direction.normalize().multiply(strength).setY(0.2));
        }
    }

    // ============ Control ============

    private void executeMessage(SpellComponent component, SpellCastContext context) {

        String text = component.param("text", "");

        if (!text.isBlank()) {
            context.caster().sendMessage(ComponentUtils.parse(text).colorIfAbsent(NamedTextColor.WHITE));
        }
    }

    private void executeCommand(SpellComponent component, SpellCastContext context) {

        String raw = component.param("command", "");

        if (raw.isBlank()) {
            return;
        }

        String casterName = context.caster().getName();
        String targetName = context.currentTargets().isEmpty() ? casterName
                : context.currentTargets().get(0).getName();

        String resolved = raw.replace("%caster%", casterName).replace("%target%", targetName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
    }

    // ============ Utilidades compartidas ============

    void triggerExplosiveIfPending(SpellCastContext context) {

        if (context.explosiveConsumed() || context.explosiveRadius() <= 0) {
            return;
        }

        context.markExplosiveConsumed();

        World world = context.currentLocation().getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.EXPLOSION, context.currentLocation(), 1);
        world.playSound(context.currentLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        double radius = context.explosiveRadius();

        for (Entity entity : world.getNearbyEntities(context.currentLocation(), radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.equals(context.caster())) {
                living.damage(context.explosiveDamage() * context.powerMultiplier(), context.caster());
            }
        }
    }

    private Location rayTraceStop(Location origin, Vector direction, double maxDistance) {

        World world = origin.getWorld();

        if (world == null) {
            return origin.clone();
        }

        var result = world.rayTraceBlocks(origin, direction, maxDistance);

        if (result == null || result.getHitPosition() == null) {
            return origin.clone().add(direction.clone().normalize().multiply(maxDistance));
        }

        Vector hit = result.getHitPosition();
        Vector back = hit.subtract(direction.clone().normalize().multiply(0.3));

        return new Location(world, back.getX(), back.getY(), back.getZ());
    }

    private LivingEntity rayTraceEntity(SpellCastContext context, double range) {

        Player caster = context.caster();
        var result = caster.getWorld().rayTraceEntities(caster.getEyeLocation(),
                caster.getLocation().getDirection(), range, 0.5, entity -> entity instanceof LivingEntity
                        && !entity.equals(caster));

        if (result == null) {
            return null;
        }

        return result.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    private Particle parseParticle(String raw) {
        try {
            return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private Sound parseSound(String raw) {
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ILLUSIONER_CAST_SPELL;
        }
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    private EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EntityType.ZOMBIE;
        }
    }

}
