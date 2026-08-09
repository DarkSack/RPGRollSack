package com.sack.rpgroll.sackeffects.engine;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.sackeffects.core.EffectDefinition;
import com.sack.rpgroll.sackeffects.core.EffectStep;
import com.sack.rpgroll.sackeffects.core.EffectTarget;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private final Plugin plugin;

    public EffectEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void play(EffectDefinition effect, EffectContext context) {

        for (EffectStep step : effect.steps()) {

            if (step.delayTicks() <= 0) {
                execute(step, context);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> execute(step, context), step.delayTicks());
            }
        }
    }

    private void execute(EffectStep step, EffectContext context) {

        try {
            switch (step.type()) {
                case PARTICLE -> executeParticle(step, context);
                case SOUND -> executeSound(step, context);
                case TITLE -> executeTitle(step, context);
                case ACTIONBAR -> executeActionBar(step, context);
                case BOSSBAR -> executeBossBar(step, context);
                case POTION -> executePotion(step, context);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error ejecutando step " + step.type() + ": " + e.getMessage());
        }
    }

    // ============ PARTICLE ============

    private void executeParticle(EffectStep step, EffectContext context) {

        Particle particle;

        try {
            particle = Particle.valueOf(step.param("particle", "FLAME").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Partícula inválida: " + step.param("particle", ""));
            return;
        }

        String shape = step.param("shape", "POINT");
        Location origin;
        Location secondary = null;

        if (shape.equalsIgnoreCase("LINE")) {
            origin = resolveOrigin(step.paramTarget("from", EffectTarget.SELF), context);
            secondary = resolveOrigin(step.paramTarget("to", EffectTarget.TARGET), context);
        } else {
            origin = resolveOrigin(step.paramTarget("target", EffectTarget.SELF), context);
        }

        if (origin == null || origin.getWorld() == null) {
            return;
        }

        int count = step.paramInt("count", 1);
        double offsetX = step.paramDouble("offset-x", 0);
        double offsetY = step.paramDouble("offset-y", 0);
        double offsetZ = step.paramDouble("offset-z", 0);
        double speed = step.paramDouble("speed", 0);

        World world = origin.getWorld();

        for (Location point : ParticleShapes.generate(step, origin, secondary)) {
            world.spawnParticle(particle, point, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    // ============ SOUND ============

    private void executeSound(EffectStep step, EffectContext context) {

        Sound sound;

        try {
            sound = Sound.valueOf(step.param("sound", "ENTITY_PLAYER_LEVELUP").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("✘ Sonido inválido: " + step.param("sound", ""));
            return;
        }

        float volume = (float) step.paramDouble("volume", 1.0);
        float pitch = (float) step.paramDouble("pitch", 1.0);

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
            // LOCATION explícita todavía no tiene sus propias coordenadas configurables acá —
            // si hace falta un punto fijo real (no relativo a un jugador), usá otro addon
            // (ej. una región) como "target" vía la API en vez de este enum.
            case LOCATION, ALL_NEARBY -> context.caster().getLocation();
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
