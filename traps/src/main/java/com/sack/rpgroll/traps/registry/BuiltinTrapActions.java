package com.sack.rpgroll.traps.registry;

import com.sack.rpgroll.traps.integration.EffectsIntegration;
import com.sack.rpgroll.traps.integration.ItemsIntegration;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tipos de acción base para trampas Y torretas (ambas ejecutan a través del
 * mismo {@link TrapActionRegistry} — ver {@code TurretEngine}): mensajes,
 * sonidos, partículas, daño, explosión custom, fuego, teletransporte,
 * efectos de estado (delega en RPGRoll-Effects), proyectiles (ráfaga fija o
 * disparo único configurable — flecha por defecto, o cualquier proyectil
 * vanilla), sonic boom (aproximación del ataque del Warden), comandos,
 * cadenas (TRIGGER_TRAP) e integración con redstone/bloques
 * (REDSTONE_PULSE, TOGGLE_BLOCKS — esta última es la base de puertas
 * secretas/paredes falsas: alterna bloques reales entre un material
 * "cerrado" y "abierto", sin packets).
 * <p>
 * DAMAGE y EFFECT actúan sobre {@code ctx.target()} (cualquier
 * {@link LivingEntity}, no solo jugador) — así una torreta puede dañar o
 * aplicar un efecto de estado a un mob hostil, no solo a un jugador.
 */
public final class BuiltinTrapActions {

    private BuiltinTrapActions() {
    }

    public static void registerAll(TrapActionRegistry registry, Plugin plugin) {

        registry.register("MESSAGE", (action, ctx) -> {
            Player target = ctx.triggeringPlayer();
            String message = action.param("value", "");
            if (target != null && !message.isBlank()) {
                target.sendMessage(ComponentUtils.parse(applyPlaceholders(message, ctx)));
            }
        });

        registry.register("SOUND", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            if (location == null) {
                return;
            }

            try {
                Sound sound = Sound.valueOf(action.param("sound", "ENTITY_TNT_PRIMED").toUpperCase(Locale.ROOT));
                float volume = Float.parseFloat(action.param("volume", "1.0"));
                float pitch = Float.parseFloat(action.param("pitch", "1.0"));
                location.getWorld().playSound(location, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SOUND inválido en acción de trampa: " + action.param("sound", ""));
            }
        });

        registry.register("PARTICLE", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            if (location == null) {
                return;
            }

            try {
                Particle particle = Particle.valueOf(action.param("particle", "SMOKE").toUpperCase(Locale.ROOT));
                int count = Integer.parseInt(action.param("count", "20"));
                location.getWorld().spawnParticle(particle, location.clone().add(0.5, 0.5, 0.5), count, 0.5, 0.5,
                        0.5, 0.02);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de trampa: " + action.param("particle", ""));
            }
        });

        registry.register("DAMAGE", (action, ctx) -> {

            LivingEntity target = ctx.target();
            if (target == null) {
                return;
            }

            double amount = Double.parseDouble(action.param("amount", "4"));
            target.damage(amount);
        });

        registry.register("HEAL", (action, ctx) -> {

            LivingEntity target = ctx.target();
            if (target == null) {
                return;
            }

            double amount = Double.parseDouble(action.param("amount", "4"));
            var maxHealthAttribute = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : target.getHealth();

            target.setHealth(Math.min(maxHealth, target.getHealth() + amount));
        });

        registry.register("EXPLOSION", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            if (location == null) {
                return;
            }

            float power = Float.parseFloat(action.param("power", "2.0"));
            boolean fire = Boolean.parseBoolean(action.param("fire", "false"));
            boolean breakBlocks = Boolean.parseBoolean(action.param("break-blocks", "false"));
            location.getWorld().createExplosion(location.clone().add(0.5, 0.5, 0.5), power, fire, breakBlocks);
        });

        registry.register("FIRE", (action, ctx) -> {
            Player target = ctx.triggeringPlayer();
            if (target != null) {
                target.setFireTicks(Integer.parseInt(action.param("ticks", "60")));
            }
        });

        registry.register("TELEPORT", (action, ctx) -> {

            Player target = ctx.triggeringPlayer();
            if (target == null) {
                return;
            }

            World world = action.params().containsKey("world")
                    ? Bukkit.getWorld(action.param("world", ""))
                    : target.getWorld();

            if (world == null) {
                return;
            }

            double x = Double.parseDouble(action.param("x", String.valueOf(target.getLocation().getX())));
            double y = Double.parseDouble(action.param("y", String.valueOf(target.getLocation().getY())));
            double z = Double.parseDouble(action.param("z", String.valueOf(target.getLocation().getZ())));

            target.teleport(new Location(world, x, y, z));
        });

        registry.register("EFFECT", (action, ctx) -> {

            LivingEntity target = ctx.target();
            String effectId = action.param("effect-id", "");

            if (target == null || effectId.isBlank()) {
                return;
            }

            if (!EffectsIntegration.apply(effectId, target)) {
                plugin.getLogger().warning("✘ No se pudo aplicar el efecto '" + effectId
                        + "' (RPGRoll-Effects ausente o efecto inexistente).");
            }
        });

        registry.register("PROJECTILE", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            Player target = ctx.triggeringPlayer();

            if (location == null || target == null) {
                return;
            }

            int amount = Integer.parseInt(action.param("amount", "1"));
            double spread = Double.parseDouble(action.param("spread", "0.1"));
            long intervalTicks = Long.parseLong(action.param("interval-ticks", "3"));
            double speed = Double.parseDouble(action.param("speed", "1.6"));

            new BukkitRunnable() {
                int fired = 0;

                @Override
                public void run() {

                    if (fired >= amount || !target.isOnline() || !target.isValid()) {
                        cancel();
                        return;
                    }

                    var direction = target.getLocation().toVector()
                            .subtract(location.toVector())
                            .normalize()
                            .add(new org.bukkit.util.Vector(
                                    (Math.random() - 0.5) * spread,
                                    (Math.random() - 0.5) * spread,
                                    (Math.random() - 0.5) * spread));

                    Projectile projectile = location.getWorld().spawn(location.clone().add(0.5, 0.5, 0.5),
                            org.bukkit.entity.Arrow.class);
                    projectile.setVelocity(direction.multiply(speed));

                    fired++;
                }
            }.runTaskTimer(plugin, 0L, Math.max(1, intervalTicks));
        });

        registry.register("CUSTOM_PROJECTILE", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            LivingEntity target = ctx.target();

            if (location == null || location.getWorld() == null || target == null) {
                return;
            }

            double speed = Double.parseDouble(action.param("speed", "2.5"));
            String projectileType = action.param("projectile-type", "ARROW").toUpperCase(Locale.ROOT);

            Vector direction = target.getLocation().add(0, target.getHeight() / 2.0, 0).toVector()
                    .subtract(location.toVector())
                    .normalize();

            if ("POTION".equals(projectileType)) {

                ThrownPotion potion = location.getWorld().spawn(location, ThrownPotion.class);

                ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potionItem.getItemMeta();

                for (PotionEffect effect : parsePotionEffects(action.param("effects", ""))) {
                    meta.addCustomEffect(effect, true);
                }

                potionItem.setItemMeta(meta);
                potion.setItem(potionItem);
                potion.setVelocity(direction.multiply(speed));
                return;
            }

            Class<? extends Projectile> projectileClass = switch (projectileType) {
                case "SPECTRAL_ARROW" -> SpectralArrow.class;
                case "SNOWBALL" -> Snowball.class;
                case "EGG" -> Egg.class;
                case "ENDER_PEARL" -> EnderPearl.class;
                case "FIREBALL" -> Fireball.class;
                case "SMALL_FIREBALL" -> SmallFireball.class;
                case "WITHER_SKULL" -> WitherSkull.class;
                case "DRAGON_FIREBALL" -> DragonFireball.class;
                case "TRIDENT" -> Trident.class;
                case "SHULKER_BULLET" -> ShulkerBullet.class;
                default -> Arrow.class;
            };

            Projectile projectile = location.getWorld().spawn(location, projectileClass);
            projectile.setVelocity(direction.multiply(speed));
        });

        registry.register("SONIC_BOOM", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            LivingEntity target = ctx.target();

            if (location == null || location.getWorld() == null || target == null) {
                return;
            }

            double amount = Double.parseDouble(action.param("damage", "10"));
            World world = location.getWorld();

            Location targetLocation = target.getLocation().add(0, target.getHeight() / 2.0, 0);
            Vector direction = targetLocation.toVector().subtract(location.toVector());
            double distance = direction.length();
            direction.normalize();

            // Partículas interpoladas entre el origen y el objetivo, simulando el "rayo" del Warden real.
            int steps = Math.max(1, (int) Math.round(distance * 2));
            for (int i = 0; i <= steps; i++) {
                Location point = location.clone().add(direction.clone().multiply(distance * i / steps));
                world.spawnParticle(Particle.SONIC_BOOM, point, 1);
            }

            world.playSound(location, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
            world.playSound(targetLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);

            target.damage(amount);
            target.setVelocity(direction.multiply(1.5).setY(0.4));
        });

        registry.register("COMMAND", (action, ctx) -> {
            String command = action.param("value", "");
            if (!command.isBlank()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlaceholders(command, ctx));
            }
        });

        registry.register("COMMAND_AS_PLAYER", (action, ctx) -> {
            Player target = ctx.triggeringPlayer();
            String command = action.param("value", "");
            if (target != null && !command.isBlank()) {
                Bukkit.dispatchCommand(target, applyPlaceholders(command, ctx));
            }
        });

        registry.register("TRIGGER_TRAP", (action, ctx) -> {

            if (ctx.engine() == null) {
                plugin.getLogger().warning("✘ TRIGGER_TRAP no tiene efecto fuera de una trampa (ej. desde una "
                        + "torreta) — no hay cadena de trampas que forzar.");
                return;
            }

            String targetPlacementId = action.param("placement-id", "");
            if (targetPlacementId.isBlank()) {
                plugin.getLogger().warning("✘ TRIGGER_TRAP sin 'placement-id' en '" + ctx.sourceId() + "'.");
                return;
            }

            ctx.engine().forceTrigger(targetPlacementId, ctx.triggeringPlayer());
        });

        registry.register("REDSTONE_PULSE", (action, ctx) -> {

            Location location = ctx.anchorLocation();
            if (location == null) {
                return;
            }

            int dx = Integer.parseInt(action.param("dx", "0"));
            int dy = Integer.parseInt(action.param("dy", "0"));
            int dz = Integer.parseInt(action.param("dz", "0"));
            int durationTicks = Integer.parseInt(action.param("duration-ticks", "20"));

            Block block = location.clone().add(dx, dy, dz).getBlock();
            Material previous = block.getType();

            block.setType(Material.REDSTONE_BLOCK);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.REDSTONE_BLOCK) {
                    block.setType(previous);
                }
            }, Math.max(1, durationTicks));
        });

        registry.register("TOGGLE_BLOCKS", (action, ctx) -> {

            Location anchor = ctx.anchorLocation();
            if (anchor == null) {
                return;
            }

            String coordsRaw = action.param("coords", "");
            if (coordsRaw.isBlank()) {
                return;
            }

            Material openMaterial = parseMaterial(action.param("open-material", "AIR"), Material.AIR);
            Material closedMaterial = parseMaterial(action.param("closed-material", "STONE_BRICKS"),
                    Material.STONE_BRICKS);
            boolean open = Boolean.parseBoolean(action.param("open", "true"));
            String animation = action.param("animation", "INSTANT").toUpperCase(Locale.ROOT);

            List<Block> blocks = new ArrayList<>();
            for (String token : coordsRaw.split(";")) {

                String[] parts = token.trim().split(",");
                if (parts.length != 3) {
                    continue;
                }

                int bx = anchor.getBlockX() + Integer.parseInt(parts[0].trim());
                int by = anchor.getBlockY() + Integer.parseInt(parts[1].trim());
                int bz = anchor.getBlockZ() + Integer.parseInt(parts[2].trim());

                blocks.add(anchor.getWorld().getBlockAt(bx, by, bz));
            }

            Material target = open ? openMaterial : closedMaterial;

            if ("SLIDE".equals(animation)) {

                new BukkitRunnable() {
                    int index = 0;

                    @Override
                    public void run() {

                        if (index >= blocks.size()) {
                            cancel();
                            return;
                        }

                        blocks.get(index).setType(target);
                        index++;
                    }
                }.runTaskTimer(plugin, 0L, 2L);

            } else {
                blocks.forEach(block -> block.setType(target));
            }
        });

    }

    private static Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Parsea {@code "TIPO:duracion-ticks:amplificador;TIPO2:duracion:amplificador"} — mismo
     * separador ";" entre entradas que ya usa TOGGLE_BLOCKS para "coords". Usado por el
     * proyectil-tipo POTION de CUSTOM_PROJECTILE (un proyectil que no hace daño de impacto,
     * solo aplica efectos de poción vanilla al estallar — igual que una poción arrojadiza real).
     */
    private static List<PotionEffect> parsePotionEffects(String raw) {

        List<PotionEffect> effects = new ArrayList<>();

        if (raw.isBlank()) {
            return effects;
        }

        for (String token : raw.split(";")) {

            String[] parts = token.trim().split(":");
            if (parts.length == 0 || parts[0].isBlank()) {
                continue;
            }

            try {
                PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
                if (type == null) {
                    continue;
                }

                int durationTicks = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 100;
                int amplifier = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;

                effects.add(new PotionEffect(type, durationTicks, amplifier));
            } catch (NumberFormatException ignored) {
            }
        }

        return effects;
    }

    private static String applyPlaceholders(String raw, TrapActionContext ctx) {

        String result = raw.replace("{trap}", ctx.definition() != null ? ctx.definition().displayName()
                : ctx.sourceId());

        Player target = ctx.triggeringPlayer();
        if (target != null) {
            result = result.replace("{player}", target.getName());
        }

        return result;
    }

}
