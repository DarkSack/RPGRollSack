package com.sack.rpgroll.dungeons.registry;

import com.sack.rpgroll.dungeons.integration.MobsIntegration;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Locale;

/**
 * Acciones base disponibles en triggers de dungeon. Cinemáticas guionadas
 * reales quedan fuera de alcance — <code>FREEZE</code> es lo más cerca
 * que llega este motor de un efecto "cinemático" (inmoviliza sin
 * necesitar un addon de cinemáticas dedicado).
 */
public final class BuiltinDungeonActions {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private BuiltinDungeonActions() {
    }

    public static void registerAll(ActionRegistry registry, Plugin plugin) {

        registry.register("MESSAGE", (action, ctx) -> {
            String message = action.param("value", "");
            if (!message.isBlank()) {
                Component component = LEGACY.deserialize(message);
                ctx.players().forEach(player -> player.sendMessage(component));
            }
        });

        registry.register("COMMAND", (action, ctx) -> {
            String command = action.param("value", "");
            if (command.isBlank()) {
                return;
            }
            Player target = ctx.targetPlayer() != null ? ctx.targetPlayer() : ctx.anyPlayer();
            String resolved = target != null ? command.replace("{player}", target.getName()) : command;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        });

        registry.register("SOUND", (action, ctx) -> {
            try {
                Sound sound = Sound.valueOf(action.param("sound", "BLOCK_NOTE_BLOCK_PLING").toUpperCase(Locale.ROOT));
                float volume = Float.parseFloat(action.param("volume", "1.0"));
                float pitch = Float.parseFloat(action.param("pitch", "1.0"));
                ctx.players().forEach(player -> player.playSound(player.getLocation(), sound, volume, pitch));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ SOUND inválido en acción de dungeon: " + action.param("sound", ""));
            }
        });

        registry.register("PARTICLE", (action, ctx) -> {
            try {
                Particle particle = Particle.valueOf(action.param("particle", "FLAME").toUpperCase(Locale.ROOT));
                int count = Integer.parseInt(action.param("count", "20"));
                Location location = resolveLocation(ctx);
                if (location != null) {
                    location.getWorld().spawnParticle(particle, location, count, 0.5, 0.5, 0.5, 0.02);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("✘ PARTICLE inválida en acción de dungeon: " + action.param("particle", ""));
            }
        });

        registry.register("TITLE", (action, ctx) -> {
            Component main = LEGACY.deserialize(action.param("title", ""));
            Component subtitle = LEGACY.deserialize(action.param("subtitle", ""));
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500));
            Title title = Title.title(main, subtitle, times);
            ctx.players().forEach(player -> player.showTitle(title));
        });

        registry.register("BOSSBAR", (action, ctx) -> {

            String text = action.param("text", "");
            int durationTicks = Integer.parseInt(action.param("duration-ticks", "100"));

            BossBar bossBar = BossBar.bossBar(LEGACY.deserialize(text), 1.0f, BossBar.Color.RED,
                    BossBar.Overlay.PROGRESS);

            ctx.players().forEach(player -> player.showBossBar(bossBar));

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> ctx.players().forEach(player -> player.hideBossBar(bossBar)), durationTicks);
        });

        registry.register("TELEPORT", (action, ctx) -> {

            Location location = resolveLocation(ctx);
            if (location == null) {
                return;
            }

            ctx.players().forEach(player -> player.teleport(location));
        });

        registry.register("WEATHER", (action, ctx) -> {

            Location location = resolveLocation(ctx);
            if (location == null || location.getWorld() == null) {
                return;
            }

            String weather = action.param("value", "clear").toLowerCase(Locale.ROOT);
            location.getWorld().setStorm(weather.equals("storm") || weather.equals("rain"));
            location.getWorld().setThundering(weather.equals("storm"));
        });

        registry.register("FREEZE", (action, ctx) -> {
            int durationTicks = Integer.parseInt(action.param("duration-ticks", "60"));
            ctx.players().forEach(player -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 250, false, false)));
        });

        registry.register("SPAWN_MOB", (action, ctx) -> {

            String mobId = action.param("mob", "");
            int amount = Integer.parseInt(action.param("amount", "1"));
            Location location = resolveLocation(ctx);

            if (mobId.isBlank() || location == null) {
                return;
            }

            for (int i = 0; i < amount; i++) {
                MobsIntegration.spawnMob(mobId, location);
            }
        });

        registry.register("EXPLOSION", (action, ctx) -> {
            Location location = resolveLocation(ctx);
            if (location == null) {
                return;
            }
            float power = Float.parseFloat(action.param("power", "1.5"));
            boolean breakBlocks = Boolean.parseBoolean(action.param("break-blocks", "false"));
            location.getWorld().createExplosion(location, power, false, breakBlocks);
        });

        registry.register("LIGHTNING", (action, ctx) -> {
            Location location = resolveLocation(ctx);
            if (location == null) {
                return;
            }
            boolean damaging = Boolean.parseBoolean(action.param("damage", "false"));
            if (damaging) {
                location.getWorld().strikeLightning(location);
            } else {
                location.getWorld().strikeLightningEffect(location);
            }
        });

    }

    private static Location resolveLocation(DungeonActionContext ctx) {

        if (ctx.targetPlayer() != null) {
            return ctx.targetPlayer().getLocation();
        }

        if (ctx.room() != null) {
            return ctx.room().entryPoint().toLocation();
        }

        Player any = ctx.anyPlayer();
        return any != null ? any.getLocation() : null;
    }

}
