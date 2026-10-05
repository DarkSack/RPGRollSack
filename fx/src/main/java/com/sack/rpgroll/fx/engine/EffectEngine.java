package com.sack.rpgroll.fx.engine;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.fx.core.EffectDefinition;
import com.sack.rpgroll.fx.core.EffectStep;
import com.sack.rpgroll.fx.core.EffectTarget;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * Ejecuta una {@link EffectDefinition}: agenda cada step al delay que le
 * corresponde (offset desde el disparo, no encadenado) y lo resuelve contra
 * el {@link EffectContext} dado. Cada tipo de step es independiente — un
 * error en uno (partícula/sonido inválido) solo loguea un warning y no
 * afecta a los demás.
 */
public class EffectEngine {

    // Los BossBar de Bukkit (org.bukkit.boss.BossBar, no el de Adventure) todavía
    // toman un String plano con códigos "§" — no aceptan Component. Se resuelve
    // acá una sola vez para no repetir la conversión &->§ en cada uso.
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    /** Tope de repeticiones por paso: un "repeat: 100000" en un YAML no debe tumbar el servidor. */
    private static final int MAX_REPEAT = 200;
    private static final Random RANDOM = new Random();

    private final Plugin plugin;

    public EffectEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void play(EffectDefinition effect, EffectContext context) {

        for (EffectStep step : effect.steps()) {

            // "repeat" + "interval": el mismo paso varias veces seguidas. Con "radius-to" o
            // "rotate" cada repetición cambia un poco, y de ahí salen las ondas que se
            // expanden y las figuras que giran.
            int repeat = Math.max(1, Math.min(MAX_REPEAT, step.paramInt("repeat", 1)));
            int interval = Math.max(1, step.paramInt("interval", 2));

            for (int i = 0; i < repeat; i++) {

                int iteration = i;
                long delay = step.delayTicks() + (long) i * interval;

                if (delay <= 0) {
                    execute(step, context, iteration, repeat);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> execute(step, context, iteration, repeat), delay);
                }
            }
        }
    }

    private void execute(EffectStep step, EffectContext context, int iteration, int repeat) {

        try {
            switch (step.type()) {
                case PARTICLE -> executeParticle(step, context, iteration, repeat);
                case SOUND -> executeSound(step, context, iteration, repeat);
                case TITLE -> executeTitle(step, context);
                case ACTIONBAR -> executeActionBar(step, context);
                case BOSSBAR -> executeBossBar(step, context);
                case POTION -> executePotion(step, context);
                case FIREWORK -> executeFirework(step, context);
                case LIGHTNING -> executeLightning(step, context);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error ejecutando step " + step.type() + ": " + e.getMessage());
        }
    }

    /** Fracción 0..1 de la repetición actual: 0 en la primera, 1 en la última. */
    private static double progress(int iteration, int repeat) {
        return repeat <= 1 ? 0 : (double) iteration / (repeat - 1);
    }

    // ============ PARTICLE ============

    private void executeParticle(EffectStep step, EffectContext context, int iteration, int repeat) {

        Particle particle;

        try {
            particle = Particle.valueOf(step.param("particle", "FLAME").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Partícula inválida: " + step.param("particle", ""));
            return;
        }

        ParticleData data = ParticleData.of(particle, step);

        if (data == null) {
            plugin.getLogger().warning("✘ La partícula " + particle + " necesita datos que FX no sabe darle");
            return;
        }

        String shape = step.param("shape", "POINT");
        Location origin;
        Location secondary = null;

        if (shape.equalsIgnoreCase("LINE")) {

            // "target" es el nombre que usa la documentación para el punto de
            // partida; "from" quedó en el código. Se aceptan los dos: escribir
            // el documentado y que no funcionara era una trampa.
            EffectTarget fromTarget = step.paramTarget("from",
                    step.paramTarget("target", EffectTarget.SELF));

            origin = resolveOrigin(fromTarget, context);
            secondary = resolveOrigin(step.paramTarget("to", EffectTarget.TARGET), context);

            // Sin objetivo, "to" cae al propio lanzador y la línea queda de
            // longitud cero: todos los puntos encima suyo, invisible. En ese
            // caso se proyecta hacia donde mira, que es lo que uno espera de
            // un rayo lanzado al aire.
            if (origin != null && secondary != null && origin.distanceSquared(secondary) < 0.01) {
                double length = step.paramDouble("length", 12.0);
                secondary = origin.clone().add(origin.getDirection().normalize().multiply(length));
            }

        } else {
            origin = resolveOrigin(step.paramTarget("target", EffectTarget.SELF), context);
        }

        if (origin == null || origin.getWorld() == null) {
            return;
        }

        // "y-offset" mueve la figura entera (a la altura del pecho, sobre la cabeza...);
        // "offset-x/y/z" siguen siendo la dispersión de cada partícula, como en vanilla.
        // "x/y/z-offset" mueven la figura entera (a la altura del pecho, sobre la cabeza...);
        // "offset-x/y/z" siguen siendo la dispersión de cada partícula, como en vanilla. Con
        // "-to" el desplazamiento cambia en cada repetición: algo que cae del cielo en diagonal.
        double t = progress(iteration, repeat);
        Vector shift = new Vector(animated(step, "x-offset", t), animated(step, "y-offset", t),
                animated(step, "z-offset", t));
        origin = origin.clone().add(shift);
        if (secondary != null) {
            secondary = secondary.clone().add(shift);
        }

        int count = step.paramInt("count", 1);
        double offsetX = step.paramDouble("offset-x", 0);
        double offsetY = step.paramDouble("offset-y", 0);
        double offsetZ = step.paramDouble("offset-z", 0);
        double speed = step.paramDouble("speed", 0);
        boolean force = Boolean.parseBoolean(step.param("force", "false"));

        double radiusFrom = step.paramDouble("radius", 1.0);
        double radius = radiusFrom
                + (step.paramDouble("radius-to", radiusFrom) - radiusFrom) * t;

        World world = origin.getWorld();
        List<Location> points = ParticleShapes.generate(step, origin, secondary, radius);

        double rotation = Math.toRadians(step.paramDouble("rotation", 0) + step.paramDouble("rotate", 0) * iteration);
        if (rotation != 0) {
            rotateAroundY(points, origin, rotation);
        }

        // "count" es por punto de la forma; con density se multiplica también,
        // para poder engordar un efecto entero desde una sola línea del YAML.
        double density = Math.max(0.1, step.paramDouble("density", 1.0));
        int perPoint = Math.max(1, (int) Math.round(count * density));

        Motion motion = Motion.parse(step.param("motion", "NONE"));
        double motionSpeed = step.paramDouble("motion-speed", 0.15);
        Location center = origin;
        int total = points.size();

        IntConsumer spawnPoint = index -> {

            Location point = points.get(index);
            Object pointData = data.forPoint(index, total, iteration);

            if (motion == Motion.NONE) {
                world.spawnParticle(particle, point, perPoint, offsetX, offsetY, offsetZ, speed, pointData, force);
                return;
            }

            // Con count 0, Minecraft usa el offset como dirección y "extra" como
            // velocidad: así cada partícula sale disparada en vez de quedarse flotando.
            Vector direction = motion.direction(center, point);

            for (int n = 0; n < perPoint; n++) {
                world.spawnParticle(particle, point, 0, direction.getX(), direction.getY(), direction.getZ(),
                        motionSpeed, pointData, force);
            }
        };

        int drawTicks = step.paramInt("draw-ticks", 0);

        if (drawTicks <= 0) {
            for (int i = 0; i < total; i++) {
                spawnPoint.accept(i);
            }
            return;
        }

        // Dibujo progresivo: la figura se traza a lo largo de varios ticks en
        // vez de aparecer entera. Es lo que hace que una hélice se vea SUBIR y
        // que un anillo se vea ABRIRSE — sin esto toda forma es estática.
        int slices = Math.min(drawTicks, total);
        int perSlice = (int) Math.ceil(total / (double) slices);

        for (int slice = 0; slice < slices; slice++) {

            int from = slice * perSlice;
            int to = Math.min(total, from + perSlice);

            if (from >= to) {
                break;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = from; i < to; i++) {
                    spawnPoint.accept(i);
                }
            }, slice);
        }
    }

    /** El valor de {@code key} interpolado hacia {@code key-to} según el avance de las repeticiones. */
    private static double animated(EffectStep step, String key, double t) {
        double from = step.paramDouble(key, 0);
        return from + (step.paramDouble(key + "-to", from) - from) * t;
    }

    private static void rotateAroundY(List<Location> points, Location origin, double angle) {

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        for (Location point : points) {

            double dx = point.getX() - origin.getX();
            double dz = point.getZ() - origin.getZ();

            point.setX(origin.getX() + dx * cos - dz * sin);
            point.setZ(origin.getZ() + dx * sin + dz * cos);
        }
    }

    /** Hacia dónde sale disparada cada partícula cuando el step lleva "motion". */
    private enum Motion {
        NONE, OUTWARD, INWARD, UP, DOWN;

        static Motion parse(String raw) {
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }

        Vector direction(Location center, Location point) {
            return switch (this) {
                case UP -> new Vector(0, 1, 0);
                case DOWN -> new Vector(0, -1, 0);
                case OUTWARD, INWARD -> {
                    Vector v = point.toVector().subtract(center.toVector());
                    if (v.lengthSquared() < 1.0E-6) {
                        v = new Vector(0, 1, 0);
                    }
                    v.normalize();
                    yield this == INWARD ? v.multiply(-1) : v;
                }
                case NONE -> new Vector();
            };
        }
    }

    // ============ SOUND ============

    private void executeSound(EffectStep step, EffectContext context, int iteration, int repeat) {

        Sound sound;

        try {
            sound = Sound.valueOf(step.param("sound", "ENTITY_PLAYER_LEVELUP").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Sonido inválido: " + step.param("sound", ""));
            return;
        }

        float volume = (float) step.paramDouble("volume", 1.0);
        // Con "repeat", "pitch-to" hace que el tono suba (o baje) en cada repetición: una carga.
        double pitchFrom = step.paramDouble("pitch", 1.0);
        float pitch = (float) (pitchFrom
                + (step.paramDouble("pitch-to", pitchFrom) - pitchFrom) * progress(iteration, repeat));

        EffectTarget target = step.paramTarget("target", EffectTarget.SELF);

        // SELF/TARGET: sonido personal (solo lo escucha esa persona).
        // LOCATION/ALL_NEARBY: sonido real de mundo (lo escucha cualquiera cerca).
        if (target == EffectTarget.SELF || target == EffectTarget.TARGET) {

            Player player = resolveSinglePlayer(target, context);
            if (player != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
            return;
        }

        Location origin = resolveOrigin(target, context);
        if (origin != null && origin.getWorld() != null) {
            origin.getWorld().playSound(origin, sound, volume, pitch);
        }
    }

    // ============ FIREWORK ============

    /**
     * Un cohete que explota al instante donde se le pide, con los colores del step. No hace
     * daño: {@link FireworkDamageListener} cancela el daño de los cohetes marcados.
     */
    private void executeFirework(EffectStep step, EffectContext context) {

        Location origin = resolveOrigin(step.paramTarget("target", EffectTarget.SELF), context);

        if (origin == null || origin.getWorld() == null) {
            return;
        }

        FireworkEffect.Type type = parseEnum(FireworkEffect.Type.class, step.param("shape", "BALL_LARGE"),
                FireworkEffect.Type.BALL_LARGE);

        List<Color> colors = ParticleData.parseColors(step.param("colors", "#FFFFFF"));
        List<Color> fade = ParticleData.parseColors(step.param("fade", ""));

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .with(type)
                .withColor(colors.isEmpty() ? List.of(Color.WHITE) : colors)
                .flicker(Boolean.parseBoolean(step.param("flicker", "false")))
                .trail(Boolean.parseBoolean(step.param("trail", "false")));

        if (!fade.isEmpty()) {
            builder.withFade(fade);
        }

        FireworkEffect effect = builder.build();
        int amount = Math.max(1, Math.min(10, step.paramInt("count", 1)));
        double spread = step.paramDouble("spread", 0);
        double yOffset = step.paramDouble("y-offset", 1.5);

        for (int i = 0; i < amount; i++) {

            Location at = origin.clone().add(
                    (RANDOM.nextDouble() * 2 - 1) * spread,
                    yOffset + RANDOM.nextDouble() * spread * 0.5,
                    (RANDOM.nextDouble() * 2 - 1) * spread);

            Firework firework = at.getWorld().spawn(at, Firework.class, entity -> {
                FireworkMeta meta = entity.getFireworkMeta();
                meta.addEffect(effect);
                meta.setPower(0);
                entity.setFireworkMeta(meta);
                entity.getPersistentDataContainer().set(FireworkDamageListener.key(plugin), PersistentDataType.BYTE,
                        (byte) 1);
            });

            firework.detonate();
        }
    }

    // ============ LIGHTNING ============

    /** Rayo solo visual (sin fuego ni daño), en el target del step. */
    private void executeLightning(EffectStep step, EffectContext context) {

        Location origin = resolveOrigin(step.paramTarget("target", EffectTarget.SELF), context);

        if (origin != null && origin.getWorld() != null) {
            origin.getWorld().strikeLightningEffect(origin);
        }
    }

    // ============ TITLE ============

    private void executeTitle(EffectStep step, EffectContext context) {

        String rawTitle = step.param("title", "");
        String rawSubtitle = step.param("subtitle", "");

        Component titleComponent = rawTitle.isBlank() ? Component.empty() : ComponentUtils.parse(rawTitle);
        Component subtitleComponent = rawSubtitle.isBlank() ? Component.empty() : ComponentUtils.parse(rawSubtitle);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(step.paramInt("fade-in", 5) * 50L),
                Duration.ofMillis(step.paramInt("stay", 40) * 50L),
                Duration.ofMillis(step.paramInt("fade-out", 10) * 50L));

        Title title = Title.title(titleComponent, subtitleComponent, times);

        for (Player player : resolvePlayerRecipients(step, context)) {
            player.showTitle(title);
        }
    }

    // ============ ACTIONBAR ============

    private void executeActionBar(EffectStep step, EffectContext context) {

        String rawText = step.param("text", "");
        Component text = rawText.isBlank() ? Component.empty() : ComponentUtils.parse(rawText);

        for (Player player : resolvePlayerRecipients(step, context)) {
            player.sendActionBar(text);
        }
    }

    // ============ BOSSBAR ============

    private void executeBossBar(EffectStep step, EffectContext context) {

        String rawTitle = step.param("title", " ");

        BarColor color = parseEnum(BarColor.class, step.param("color", "WHITE"), BarColor.WHITE);
        BarStyle style = parseEnum(BarStyle.class, step.param("style", "SOLID"), BarStyle.SOLID);
        double progress = clamp(step.paramDouble("progress", 1.0), 0, 1);
        int durationTicks = step.paramInt("duration", 60);

        BossBar bossBar = Bukkit.createBossBar(legacyPlain(rawTitle), color, style);
        bossBar.setProgress(progress);

        List<Player> recipients = resolvePlayerRecipients(step, context);

        for (Player player : recipients) {
            bossBar.addPlayer(player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : recipients) {
                bossBar.removePlayer(player);
            }
        }, Math.max(1, durationTicks));
    }

    private String legacyPlain(String raw) {
        if (raw == null || raw.isBlank()) {
            return " ";
        }
        return LEGACY_SECTION.serialize(ComponentUtils.parse(raw));
    }

    // ============ POTION ============

    private void executePotion(EffectStep step, EffectContext context) {

        PotionEffectType type = resolvePotionType(step.param("potion", "SPEED"));

        if (type == null) {
            plugin.getLogger().warning("✘ Efecto de poción inválido: " + step.param("potion", ""));
            return;
        }

        int durationTicks = step.paramInt("duration", 100);
        int amplifier = step.paramInt("amplifier", 0);

        PotionEffect potionEffect = new PotionEffect(type, durationTicks, amplifier, false, true);

        for (LivingEntity entity : resolveLivingRecipients(step, context)) {
            entity.addPotionEffect(potionEffect);
        }
    }

    private PotionEffectType resolvePotionType(String raw) {
        try {
            return PotionEffectType.getByName(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    // ============ Resolución de ubicación/objetivos ============

    /** Una única Location — usada por PARTICLE (origen/secundario) y SOUND (LOCATION/ALL_NEARBY). */
    private Location resolveOrigin(EffectTarget target, EffectContext context) {
        return switch (target) {
            case SELF -> context.caster().getLocation();
            case TARGET -> {
                Location targetLocation = context.resolvedTargetLocation();
                yield targetLocation != null ? targetLocation : context.caster().getLocation();
            }
            // LOCATION es la ubicación que se le pasó a la API — la del impacto
            // de un proyectil, la de una trampa, la de un mob. Antes devolvía la
            // del lanzador, así que un efecto de impacto se dibujaba encima del
            // jugador en vez de donde pegó.
            case LOCATION -> {
                Location passed = context.resolvedTargetLocation();
                yield passed != null ? passed : context.caster().getLocation();
            }
            // ALL_NEARBY se ancla en quien lanza: su radio se mide desde ahí.
            case ALL_NEARBY -> context.caster().getLocation();
        };
    }

    private Player resolveSinglePlayer(EffectTarget target, EffectContext context) {
        return switch (target) {
            case SELF -> context.caster();
            case TARGET -> context.targetEntity() instanceof Player player ? player : null;
            default -> null;
        };
    }

    /** Para title/actionbar/bossbar — solo tiene sentido mandarlos a jugadores reales. */
    private List<Player> resolvePlayerRecipients(EffectStep step, EffectContext context) {

        EffectTarget target = step.paramTarget("target", EffectTarget.SELF);

        return switch (target) {
            case SELF -> List.of(context.caster());
            case TARGET -> context.targetEntity() instanceof Player player ? List.of(player) : List.of();
            case ALL_NEARBY -> nearbyPlayers(step, context);
            case LOCATION -> List.of();
        };
    }

    /** Para POTION — cualquier LivingEntity (jugador o mob) puede recibirlo. */
    private List<LivingEntity> resolveLivingRecipients(EffectStep step, EffectContext context) {

        EffectTarget target = step.paramTarget("target", EffectTarget.SELF);

        return switch (target) {
            case SELF -> List.of(context.caster());
            case TARGET -> context.targetEntity() instanceof LivingEntity living ? List.of(living) : List.of();
            case ALL_NEARBY -> {
                List<LivingEntity> result = new ArrayList<>();
                Location center = resolveOrigin(step.paramTarget("around", EffectTarget.SELF), context);
                double radius = step.paramDouble("radius", 10);

                if (center != null && center.getWorld() != null) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (entity instanceof LivingEntity living) {
                            result.add(living);
                        }
                    }
                }

                yield result;
            }
            case LOCATION -> List.of();
        };
    }

    private List<Player> nearbyPlayers(EffectStep step, EffectContext context) {

        Location center = resolveOrigin(step.paramTarget("around", EffectTarget.SELF), context);
        double radius = step.paramDouble("radius", 10);

        if (center == null || center.getWorld() == null) {
            return List.of();
        }

        double radiusSquared = radius * radius;
        List<Player> result = new ArrayList<>();

        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                result.add(player);
            }
        }

        return result;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

}
