package com.sack.rpgroll.items.registry;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Locale;

/**
 * Tipos de acción base disponibles para triggers/habilidades de ítem:
 * comandos, sonidos, partículas, explosiones, invocaciones, daño, curación,
 * mensajes, bossbars, títulos y proyectiles. Cinemáticas, abrir GUI y
 * scripts quedan como puntos de extensión — un addon los registra si los
 * necesita, no vienen implementados acá (requieren un motor propio ajeno
 * al alcance de este addon).
 */
public final class BuiltinItemActions {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private BuiltinItemActions() {
    }

    public static void registerAll(ActionRegistry registry, Plugin plugin) {

        registry.register("MESSAGE", (action, ctx) -> {
            String message = action.param("value", "");
            if (!message.isBlank()) {
                ctx.player().sendMessage(LEGACY.deserialize(applyPlaceholders(message, ctx)));
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
                Sound sound = Sound.valueOf(action.param("sound", "ENTITY_PLAYER_HURT").toUpperCase(Locale.ROOT));
                float volume = Float.parseFloat(action.param("volume", "1.0"));
                float pitch = Float.parseFloat(action.param("pitch", "1.0"));
                ctx.player().playSound(ctx.player().getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SOUND inválido en acción de ítem: " + action.param("sound", ""));
            }
        });

        registry.register("PARTICLE", (action, ctx) -> {

            try {
                Particle particle = Particle.valueOf(action.param("particle", "FLAME").toUpperCase(Locale.ROOT));
                int count = Integer.parseInt(action.param("count", "20"));
                Location location = targetLocation(ctx);
                location.getWorld().spawnParticle(particle, location, count, 0.3, 0.5, 0.3, 0.01);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de ítem: " + action.param("particle", ""));
            }
        });

        registry.register("EXPLOSION", (action, ctx) -> {

            Location location = targetLocation(ctx);
            float power = Float.parseFloat(action.param("power", "1.0"));
            boolean breakBlocks = Boolean.parseBoolean(action.param("break-blocks", "false"));
            location.getWorld().createExplosion(location, power, false, breakBlocks);
        });

        registry.register("DAMAGE", (action, ctx) -> {

            if (ctx.target() == null) {
                return;
            }

            double amount = Double.parseDouble(action.param("amount", "1"));
            ctx.target().damage(amount, ctx.player());
        });

        registry.register("HEAL", (action, ctx) -> {

            double amount = Double.parseDouble(action.param("amount", "1"));
            Player player = ctx.player();

            var maxHealthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;

            player.setHealth(Math.min(maxHealth, player.getHealth() + amount));
        });

        registry.register("FIRE", (action, ctx) -> {

            Entity receiver = ctx.target() != null ? ctx.target() : ctx.player();
            int ticks = Integer.parseInt(action.param("ticks", "60"));
            receiver.setFireTicks(ticks);
        });

        registry.register("TITLE", (action, ctx) -> {

            Component main = LEGACY.deserialize(action.param("title", ""));
            Component subtitle = LEGACY.deserialize(action.param("subtitle", ""));
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
            ctx.player().showTitle(Title.title(main, subtitle, times));
        });

        registry.register("BOSSBAR", (action, ctx) -> {

            Component text = LEGACY.deserialize(action.param("text", ""));
            BossBar bossBar = BossBar.bossBar(text, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

            ctx.player().showBossBar(bossBar);

            long durationTicks = Long.parseLong(action.param("duration-ticks", "100"));
            new BukkitRunnable() {
                @Override
                public void run() {
                    ctx.player().hideBossBar(bossBar);
                }
            }.runTaskLater(plugin, durationTicks);
        });

        registry.register("SUMMON", (action, ctx) -> {

            try {
                EntityType type = EntityType.valueOf(action.param("entity", "ZOMBIE").toUpperCase(Locale.ROOT));
                int amount = Integer.parseInt(action.param("amount", "1"));
                Location location = ctx.player().getLocation();

                for (int i = 0; i < amount; i++) {
                    location.getWorld().spawnEntity(location, type);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SUMMON inválido en acción de ítem: " + action.param("entity", ""));
            }
        });

        registry.register("PROJECTILE", (action, ctx) -> {

            try {
                @SuppressWarnings("unchecked")
                Class<? extends Projectile> projectileType = (Class<? extends Projectile>) Class.forName(
                        "org.bukkit.entity." + action.param("projectile", "Arrow"));

                Player player = ctx.player();
                double speed = Double.parseDouble(action.param("speed", "2.0"));

                Projectile projectile = player.launchProjectile(projectileType);
                projectile.setVelocity(player.getLocation().getDirection().multiply(speed));
            } catch (ReflectiveOperationException | ClassCastException e) {
                plugin.getLogger().warning("✘ PROJECTILE inválido en acción de ítem: "
                        + action.param("projectile", ""));
            }
        });
    }

    private static Location targetLocation(ItemActionContext ctx) {
        LivingEntity target = ctx.target();
        return target != null ? target.getLocation() : ctx.player().getLocation();
    }

    private static String applyPlaceholders(String raw, ItemActionContext ctx) {

        String result = raw.replace("{player}", ctx.player().getName());

        if (ctx.target() instanceof Player targetPlayer) {
            result = result.replace("{target}", targetPlayer.getName());
        }

        return result;
    }

}
