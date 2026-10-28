package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Locale;

/**
 * Tipos de acción base disponibles para triggers/habilidades de ítem:
 * comandos, sonidos, partículas, explosiones, invocaciones, daño, curación,
 * mensajes, bossbars, títulos, proyectiles y disparos. Cinemáticas, abrir GUI y
 * scripts quedan como puntos de extensión — un addon los registra si los
 * necesita, no vienen implementados acá (requieren un motor propio ajeno
 * al alcance de este addon).
 */
public final class BuiltinItemActions {


    private BuiltinItemActions() {
    }

    public static void registerAll(ActionRegistry registry, Plugin plugin) {

        registry.register("MESSAGE", (action, ctx) -> {
            String message = action.param("value", "");
            if (!message.isBlank()) {
                ctx.player().sendMessage(ComponentUtils.parse(applyPlaceholders(message, ctx)));
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
                // Algunas partículas exigen un dato (DRAGON_BREATH, la potencia desde 1.21.9): sin él
                // spawnParticle lanza IllegalArgumentException y la acción se perdía con un aviso.
                Object data = particle.getDataType() == Float.class ? 1.0f : null;
                if (data == null && particle.getDataType() != Void.class) {
                    throw new IllegalArgumentException("la partícula necesita datos: " + particle);
                }
                location.getWorld().spawnParticle(particle, location, count, 0.3, 0.5, 0.3, 0.01, data);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de ítem: " + action.param("particle", ""));
            }
        });

        /**
         * Reproduce un efecto completo de RPGRoll-FX por id.
         * <p>
         * A diferencia de PARTICLE, que dibuja una sola partícula vanilla, acá
         * el efecto trae sus propias formas, sonidos y tiempos ya compuestos.
         * Integración blanda: sin RPGRoll-FX instalado no hace nada.
         */
        registry.register("PARTICLES", (action, ctx) -> {

            String effectId = action.param("effect-id", action.param("id", ""));

            if (effectId.isBlank()) {
                return;
            }

            com.sack.rpgroll.common.integration.ParticlesIntegration.play(
                    effectId, ctx.player(), targetLocation(ctx));
        });

        registry.register("EXPLOSION", (action, ctx) -> {

            Location location = targetLocation(ctx);
            float power = Float.parseFloat(action.param("power", "1.0"));
            boolean breakBlocks = Boolean.parseBoolean(action.param("break-blocks", "false"));
            // Con el jugador como fuente: la explosión no le hace daño a él
            // (vanilla excluye a la fuente), las muertes se le atribuyen y
            // WorldGuard/GriefPrevention saben de quién es. Sin fuente, la
            // habilidad hería a quien la lanzaba.
            location.getWorld().createExplosion(location, power, false, breakBlocks, ctx.player());
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

            player.heal(amount);
        });

        registry.register("FIRE", (action, ctx) -> {

            Entity receiver = ctx.target() != null ? ctx.target() : ctx.player();
            int ticks = Integer.parseInt(action.param("ticks", "60"));
            receiver.setFireTicks(ticks);
        });

        registry.register("TITLE", (action, ctx) -> {

            Component main = ComponentUtils.parse(action.param("title", ""));
            Component subtitle = ComponentUtils.parse(action.param("subtitle", ""));
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
            ctx.player().showTitle(Title.title(main, subtitle, times));
        });

        registry.register("BOSSBAR", (action, ctx) -> {

            Component text = ComponentUtils.parse(action.param("text", ""));
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
                if (projectile instanceof AbstractArrow arrow) {
                    // Si no, la flecha se recoge del suelo: una habilidad con cooldown corto daba flechas infinitas.
                    arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                plugin.getLogger().warning("✘ PROJECTILE inválido en acción de ítem: "
                        + action.param("projectile", ""));
            }
        });

        /**
         * Disparo de arma de fuego o de energía, a nombre del jugador. Sin {@code speed} es un rayo
         * instantáneo desde los ojos que daña a la primera criatura que toca; con {@code speed}
         * (bloques por tick) es un proyectil que vuela, con {@code gravity} y el modelo {@code model}
         * (ver {@link ShotProjectile}). La estela la pinta {@link ShotVisuals} y el impacto
         * (daño, explosión que puede romper bloques y {@code hit-effect}) {@link ShotImpact};
         * {@code muzzle-effect} es el fogonazo de RPGRoll-FX en la boca del arma.
         */
        registry.register("SHOOT", (action, ctx) -> shoot(action, ctx, plugin));
    }

    static void shoot(ItemAction action, ItemActionContext ctx, Plugin plugin) {

        Player player = ctx.player();
        double range = Double.parseDouble(action.param("range", "32"));
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();

        ShotVisuals visuals;
        try {
            visuals = ShotVisuals.from(action, eye.toVector());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ SHOOT con partícula inválida: " + action.param("particle", "")
                    + " / " + action.param("particle2", ""));
            return;
        }
        ShotImpact impact = ShotImpact.from(action);
        Location barrel = barrel(eye, direction);
        String muzzle = action.param("muzzle-effect", "");
        if (!muzzle.isBlank()) {
            com.sack.rpgroll.common.integration.ParticlesIntegration.play(muzzle, player, barrel);
        }

        double speed = Double.parseDouble(action.param("speed", "0"));
        if (speed > 0) {
            ShotProjectile.launch(plugin, player, barrel, direction, speed,
                    Double.parseDouble(action.param("gravity", "0")), range, action.param("model", ""), visuals, impact);
            return;
        }

        RayTraceResult hit = player.getWorld().rayTrace(eye, direction, range, FluidCollisionMode.NEVER, true, 0.3,
                entity -> entity instanceof LivingEntity && !entity.equals(player));
        Vector end = hit != null ? hit.getHitPosition() : eye.toVector().add(direction.clone().multiply(range));
        visuals.trail(player.getWorld(), barrel.toVector(), end);
        if (hit != null) {
            impact.apply(player, end.toLocation(player.getWorld()),
                    hit.getHitEntity() instanceof LivingEntity living ? living : null);
        }
    }

    /** La boca del arma en la mano derecha: un poco delante, a la derecha y por debajo de los ojos. */
    static Location barrel(Location eye, Vector direction) {
        Vector right = direction.getCrossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1e-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        Vector down = right.getCrossProduct(direction).normalize().multiply(-1);
        return eye.clone().add(direction.clone().multiply(1.0)).add(right.multiply(0.32)).add(down.multiply(0.22));
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

        if (ctx.target() != null) {
            // Vale para cualquier entidad en /effect, /damage, etc.; {target} solo para jugadores.
            result = result.replace("{target_uuid}", ctx.target().getUniqueId().toString());
        }

        return result;
    }

}
