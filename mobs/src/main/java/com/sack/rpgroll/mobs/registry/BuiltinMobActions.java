package com.sack.rpgroll.mobs.registry;

import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Locale;

/**
 * Tipos de acción base para triggers/skills de mob: mensajes, sonidos,
 * partículas, comandos, daño, curación, invocar aliados, teletransporte,
 * explosión, fuego, congelar, empujar/atraer, rayos y títulos. Cinemáticas
 * y "abrir GUI" quedan como puntos de extensión — un addon los registra
 * si los necesita.
 */
public final class BuiltinMobActions {


    private BuiltinMobActions() {
    }

    public static void registerAll(ActionRegistry registry, Plugin plugin) {

        registry.register("MESSAGE", (action, ctx) -> {
            Player target = ctx.targetPlayer();
            String message = action.param("value", "");
            if (target != null && !message.isBlank()) {
                target.sendMessage(ComponentUtils.parse(applyPlaceholders(message, ctx)));
            }
        });

        registry.register("COMMAND", (action, ctx) -> {
            String command = action.param("value", "");
            if (!command.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlaceholders(command, ctx));
            }
        });

        registry.register("SOUND", (action, ctx) -> {

            try {
                Sound sound = Sound.valueOf(action.param("sound", "ENTITY_ENDERMAN_SCREAM").toUpperCase(Locale.ROOT));
                float volume = Float.parseFloat(action.param("volume", "1.0"));
                float pitch = Float.parseFloat(action.param("pitch", "1.0"));
                ctx.mob().getWorld().playSound(ctx.mob().getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SOUND inválido en acción de mob: " + action.param("sound", ""));
            }
        });

        registry.register("PARTICLE", (action, ctx) -> {

            try {
                Particle particle = Particle.valueOf(action.param("particle", "FLAME").toUpperCase(Locale.ROOT));
                int count = Integer.parseInt(action.param("count", "20"));
                Location location = ctx.mob().getLocation().add(0, 1, 0);
                location.getWorld().spawnParticle(particle, location, count, 0.5, 0.5, 0.5, 0.02);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de mob: " + action.param("particle", ""));
            }
        });

        registry.register("DAMAGE", (action, ctx) -> {
            if (ctx.target() != null) {
                double amount = Double.parseDouble(action.param("amount", "1"));
                ctx.target().damage(amount, ctx.mob());
            }
        });

        registry.register("HEAL", (action, ctx) -> {

            double amount = Double.parseDouble(action.param("amount", "1"));
            LivingEntity healed = "target".equalsIgnoreCase(action.param("apply-to", "self"))
                    ? ctx.target() : ctx.mob();

            if (healed == null) {
                return;
            }

            var maxHealthAttribute = healed.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
            healed.setHealth(Math.min(maxHealth, healed.getHealth() + amount));
        });

        registry.register("SUMMON", (action, ctx) -> {

            try {
                EntityType type = EntityType.valueOf(action.param("entity", "ZOMBIE").toUpperCase(Locale.ROOT));
                int amount = Integer.parseInt(action.param("amount", "1"));
                Location location = ctx.mob().getLocation();

                for (int i = 0; i < amount; i++) {
                    location.getWorld().spawnEntity(location, type);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SUMMON inválido en acción de mob: " + action.param("entity", ""));
            }
        });

        registry.register("TELEPORT", (action, ctx) -> {

            String mode = action.param("to", "target");

            if (mode.equalsIgnoreCase("target") && ctx.target() != null) {
                ctx.mob().teleport(ctx.target().getLocation());
            } else if (mode.equalsIgnoreCase("self_to_target") && ctx.target() != null) {
                ctx.target().teleport(ctx.mob().getLocation());
            }
        });

        registry.register("EXPLOSION", (action, ctx) -> {
            Location location = ctx.mob().getLocation();
            float power = Float.parseFloat(action.param("power", "1.5"));
            boolean breakBlocks = Boolean.parseBoolean(action.param("break-blocks", "false"));
            location.getWorld().createExplosion(location, power, false, breakBlocks);
        });

        registry.register("FIRE", (action, ctx) -> {
            Entity receiver = ctx.target() != null ? ctx.target() : ctx.mob();
            int ticks = Integer.parseInt(action.param("ticks", "60"));
            receiver.setFireTicks(ticks);
        });

        registry.register("FREEZE", (action, ctx) -> {

            if (!(ctx.target() instanceof LivingEntity target)) {
                return;
            }

            int duration = Integer.parseInt(action.param("duration-ticks", "60"));
            int amplifier = Integer.parseInt(action.param("amplifier", "2"));

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier));
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3);
        });

        registry.register("PUSH", (action, ctx) -> {

            if (!(ctx.target() instanceof LivingEntity target)) {
                return;
            }

            double strength = Double.parseDouble(action.param("strength", "1.5"));
            Vector direction = target.getLocation().toVector().subtract(ctx.mob().getLocation().toVector())
                    .normalize().multiply(strength).setY(0.3);

            target.setVelocity(direction);
        });

        registry.register("PULL", (action, ctx) -> {

            if (!(ctx.target() instanceof LivingEntity target)) {
                return;
            }

            double strength = Double.parseDouble(action.param("strength", "1.5"));
            Vector direction = ctx.mob().getLocation().toVector().subtract(target.getLocation().toVector())
                    .normalize().multiply(strength).setY(0.2);

            target.setVelocity(direction);
        });

        registry.register("LIGHTNING", (action, ctx) -> {
            Location location = ctx.target() != null ? ctx.target().getLocation() : ctx.mob().getLocation();
            boolean damaging = Boolean.parseBoolean(action.param("damage", "true"));

            if (damaging) {
                location.getWorld().strikeLightning(location);
            } else {
                location.getWorld().strikeLightningEffect(location);
            }
        });

        registry.register("TITLE", (action, ctx) -> {

            Player target = ctx.targetPlayer();
            if (target == null) {
                return;
            }

            Component main = ComponentUtils.parse(action.param("title", ""));
            Component subtitle = ComponentUtils.parse(action.param("subtitle", ""));
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));

            target.showTitle(Title.title(main, subtitle, times));
        });

    }

    private static String applyPlaceholders(String raw, MobActionContext ctx) {

        String result = raw.replace("{mob}", ctx.definition().displayName());

        Player targetPlayer = ctx.targetPlayer();
        if (targetPlayer != null) {
            result = result.replace("{target}", targetPlayer.getName());
        }

        return result;
    }

}
