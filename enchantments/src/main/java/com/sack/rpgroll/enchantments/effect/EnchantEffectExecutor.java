package com.sack.rpgroll.enchantments.effect;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.enchantments.core.EnchantEffect;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

/**
 * Ejecuta la lista de efectos de un encantamiento una vez que su trigger,
 * probabilidad y condiciones se cumplieron. Los valores numéricos de cada
 * efecto pueden ser literales o placeholders "{clave}" que se resuelven
 * contra el bloque de datos del nivel actual (levels.&lt;n&gt;.&lt;clave&gt; del YAML).
 */
public class EnchantEffectExecutor {

    private static final double DEFAULT_PICKUP_RADIUS = 6.0;

    private final Plugin plugin;
    private final BlockEffects blockEffects;
    private final AutoSmelt autoSmelt = new AutoSmelt();
    private final GearEffects gearEffects = new GearEffects();

    public EnchantEffectExecutor(Plugin plugin) {
        this.plugin = plugin;
        this.blockEffects = new BlockEffects(plugin);
    }

    /** Listener que convierte los drops marcados por AUTO_SMELT; hay que registrarlo. */
    public AutoSmelt autoSmelt() {
        return autoSmelt;
    }

    public void execute(List<EnchantEffect> effects, EffectContext context) {
        for (EnchantEffect effect : effects) {
            executeOne(effect, context);
        }
    }

    private void executeOne(EnchantEffect effect, EffectContext context) {
        switch (effect.type()) {
            case DAMAGE -> executeDamage(effect, context);
            case HEAL -> executeHeal(effect, context);
            case LIGHTNING -> executeLightning(effect, context);
            case POTION -> executePotion(effect, context);
            case FIRE -> executeFire(effect, context);
            case EXPLOSION -> executeExplosion(effect, context);
            case TELEPORT -> executeTeleport(effect, context);
            case COMMAND -> executeCommand(effect, context);
            case MESSAGE -> executeMessage(effect, context);
            case PARTICLE -> executeParticle(effect, context);
            case PARTICLES -> executeParticles(effect, context);
            case SOUND -> executeSound(effect, context);
            case PICKUP_ITEMS -> executePickupItems(effect, context);
            case VEIN_MINE -> blockEffects.veinMine(context, (int) resolveDouble(effect, context, "max", 32));
            case TREE_FELL -> blockEffects.treeFell(context, (int) resolveDouble(effect, context, "max", 64),
                    Boolean.parseBoolean(effect.param("leaves", "true")));
            case AREA_MINE -> blockEffects.areaMine(context, (int) resolveDouble(effect, context, "radius", 1));
            case AUTO_SMELT -> autoSmelt.mark(context);
            case TILL_AREA -> blockEffects.tillArea(context, (int) resolveDouble(effect, context, "radius", 1),
                    Boolean.parseBoolean(effect.param("replant", "true")));
            case REPAIR -> gearEffects.repair(context, resolveDouble(effect, context, "ratio", 3),
                    resolveDouble(effect, context, "equipment", 0) > 0);
            case DAMAGE_BONUS -> gearEffects.damageBonus(context, resolveDouble(effect, context, "amount", 1),
                    resolveDouble(effect, context, "multiplier", 1));
            case KNOCKBACK -> gearEffects.knockback(context, resolveDouble(effect, context, "strength", 0.8),
                    resolveDouble(effect, context, "vertical", 0.3));
            case REFLECT -> gearEffects.reflect(context, resolveDouble(effect, context, "speed", 1.6),
                    resolveDouble(effect, context, "chance", 100));
            case SHIELD_COOLDOWN -> gearEffects.shieldCooldown(context,
                    resolveDouble(effect, context, "factor", 0.5));
        }
    }

    private void executeDamage(EnchantEffect effect, EffectContext context) {

        if (context.target() == null) {
            return;
        }

        double amount = resolveDouble(effect, context, "amount", 1.0);
        context.target().damage(amount, context.player());
    }

    private void executeHeal(EnchantEffect effect, EffectContext context) {

        double amount = resolveDouble(effect, context, "amount", 1.0);
        Player player = context.player();

        var maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;

        player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
    }

    private void executeLightning(EnchantEffect effect, EffectContext context) {

        Location location = targetLocation(context);
        boolean damaging = Boolean.parseBoolean(effect.param("damage", "false"));
        World world = location.getWorld();

        if (damaging) {
            world.strikeLightning(location);
        } else {
            world.strikeLightningEffect(location);
        }
    }

    private void executePotion(EnchantEffect effect, EffectContext context) {

        String rawName = effect.param("potion", "SPEED").toLowerCase(Locale.ROOT);
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(rawName));

        if (type == null) {
            plugin.getLogger().warning("✘ Tipo de poción inválido en efecto de encantamiento: " + rawName);
            return;
        }

        int duration = (int) resolveDouble(effect, context, "duration", 100);
        int amplifier = (int) resolveDouble(effect, context, "amplifier", 0);

        LivingEntity receiver = "target".equalsIgnoreCase(effect.param("apply-to", "self"))
                ? context.target()
                : context.player();

        if (receiver == null) {
            return;
        }

        receiver.addPotionEffect(new PotionEffect(type, duration, amplifier));
    }

    private void executeFire(EnchantEffect effect, EffectContext context) {

        Entity receiver = "self".equalsIgnoreCase(effect.param("apply-to", "target"))
                ? context.player()
                : context.target();

        if (receiver == null) {
            return;
        }

        int ticks = (int) resolveDouble(effect, context, "ticks", 60);
        receiver.setFireTicks(ticks);
    }

    private void executeExplosion(EnchantEffect effect, EffectContext context) {

        Location location = targetLocation(context);
        float power = (float) resolveDouble(effect, context, "power", 1.0);
        boolean breakBlocks = Boolean.parseBoolean(effect.param("break-blocks", "false"));

        // El jugador como fuente, por lo mismo que en las acciones de ítems:
        // sin ella, el encantamiento dañaba a su propio portador.
        location.getWorld().createExplosion(location, power, false, breakBlocks, context.player());
    }

    private void executeTeleport(EnchantEffect effect, EffectContext context) {

        String mode = effect.param("to", "target");

        if (mode.equalsIgnoreCase("target") && context.target() != null) {
            context.player().teleport(context.target().getLocation());
        } else if (mode.equalsIgnoreCase("spawn")) {
            context.player().teleport(context.player().getWorld().getSpawnLocation());
        }
    }

    private void executeCommand(EnchantEffect effect, EffectContext context) {

        String command = effect.param("value", "");
        if (command.isBlank()) {
            return;
        }

        command = applyPlaceholders(command, context);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void executeMessage(EnchantEffect effect, EffectContext context) {

        String message = effect.param("value", "");
        if (message.isBlank()) {
            return;
        }

        message = applyPlaceholders(message, context);
        context.player().sendMessage(ComponentUtils.parse(message));
    }

    private String applyPlaceholders(String raw, EffectContext context) {

        String result = raw.replace("{player}", context.player().getName())
                .replace("{level}", String.valueOf(context.level()));

        if (context.target() instanceof Player targetPlayer) {
            result = result.replace("{target}", targetPlayer.getName());
        }

        return result;
    }

    /**
     * Reproduce un efecto completo de RPGRoll-FX por id.
     * <p>
     * PARTICLE dibuja una partícula vanilla suelta; PARTICLES dispara un
     * efecto ya compuesto. Integración blanda: sin el plugin no hace nada.
     */
    private void executeParticles(EnchantEffect effect, EffectContext context) {

        String effectId = effect.param("effect-id", effect.param("id", ""));

        if (effectId.isBlank()) {
            return;
        }

        com.sack.rpgroll.common.integration.ParticlesIntegration.play(
                effectId, context.player(), targetLocation(context));
    }

    private void executeParticle(EnchantEffect effect, EffectContext context) {

        Particle particle;

        try {
            particle = Particle.valueOf(effect.param("particle", "FLAME").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Partícula inválida en efecto de encantamiento: "
                    + effect.param("particle", ""));
            return;
        }

        int count = (int) resolveDouble(effect, context, "count", 20);
        Location location = targetLocation(context);

        location.getWorld().spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.01);
    }

    private void executeSound(EnchantEffect effect, EffectContext context) {

        Sound sound;

        try {
            sound = Sound.valueOf(effect.param("sound", "ENTITY_PLAYER_HURT").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Sonido inválido en efecto de encantamiento: " + effect.param("sound", ""));
            return;
        }

        float volume = (float) resolveDouble(effect, context, "volume", 1.0);
        float pitch = (float) resolveDouble(effect, context, "pitch", 1.0);

        context.player().playSound(context.player().getLocation(), sound, volume, pitch);
    }

    private void executePickupItems(EnchantEffect effect, EffectContext context) {

        double radius = resolveDouble(effect, context, "radius", DEFAULT_PICKUP_RADIUS);
        Player player = context.player();

        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {

            if (!(nearby instanceof Item itemEntity)) {
                continue;
            }

            var leftover = player.getInventory().addItem(itemEntity.getItemStack());

            if (leftover.isEmpty()) {
                itemEntity.remove();
            } else {
                itemEntity.setItemStack(leftover.values().iterator().next());
            }
        }
    }

    private Location targetLocation(EffectContext context) {
        return context.target() != null ? context.target().getLocation() : context.player().getLocation();
    }

    private double resolveDouble(EnchantEffect effect, EffectContext context, String key, double fallback) {

        String raw = effect.params().get(key);

        if (raw == null) {
            return fallback;
        }

        if (raw.startsWith("{") && raw.endsWith("}")) {
            Double fromLevel = context.levelData().get(raw.substring(1, raw.length() - 1));
            return fromLevel != null ? fromLevel : fallback;
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
