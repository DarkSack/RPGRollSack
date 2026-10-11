package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Canalización para hechizos con trigger HOLD. Bukkit no expone un evento
 * de "seguís apretando click derecho" (eso requeriría escuchar paquetes) —
 * en cambio, al iniciarse la canalización el jugador tiene {@code
 * castTimeTicks} para completarla quieto y sin recibir daño; moverse más
 * de un par de bloques o ser dañado la cancela. Es la misma simplificación
 * que usan la mayoría de los plugins de RPG/magia sobre Bukkit puro.
 */
public class SpellChannelManager implements Listener {

    private static final double CANCEL_MOVE_DISTANCE_SQUARED = 0.09;
    private static final int CHANNEL_STRANDS = 3;

    private final Plugin plugin;
    private final SpellCastEngine engine;
    private final LangManager lang;
    private final Map<UUID, BukkitTask> channeling = new HashMap<>();

    public SpellChannelManager(Plugin plugin, SpellCastEngine engine, LangManager lang) {
        this.plugin = plugin;
        this.engine = engine;
        this.lang = lang;
    }

    public boolean isChanneling(UUID uuid) {
        return channeling.containsKey(uuid);
    }

    public void start(Player player, Spell spell, PlayerSpellbook spellbook, SpellCatalyst catalyst) {

        if (isChanneling(player.getUniqueId())) {
            return;
        }

        Location startLocation = player.getLocation();
        int totalTicks = Math.max(1, spell.castTimeTicks());

        lang.send(player, "channel.starting", "spell", spell.displayName());

        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {

            int elapsed = 0;

            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancelChannel(player.getUniqueId());
                    return;
                }

                if (player.getLocation().distanceSquared(startLocation) > CANCEL_MOVE_DISTANCE_SQUARED) {
                    lang.send(player, "channel.cancelled_move");
                    cancelChannel(player.getUniqueId());
                    return;
                }

                elapsed++;
                int percent = (int) (100.0 * elapsed / totalTicks);
                player.sendActionBar(lang.component("channel.progress", "percent", percent));
                drawChannel(player, spell, elapsed, totalTicks);

                if (elapsed >= totalTicks) {

                    // Hay que parar la tarea, no solo olvidarla: seguía lanzando el hechizo cada tick.
                    cancelChannel(player.getUniqueId());
                    CastResult result = engine.cast(spell, player, spellbook, catalyst);

                    if (!result.success()) {
                        result.reasons().forEach(reason -> player.sendMessage(
                                ComponentUtils.parseWithDefault("✘ " + reason, NamedTextColor.RED)));
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        channeling.put(player.getUniqueId(), task);
    }

    /**
     * Mientras canaliza: tres hilos de partículas del color del hechizo que suben en
     * espiral y se cierran sobre el jugador, y un sonido que sube de tono. Antes solo
     * había un porcentaje en la barra de acción y nada indicaba a los demás que
     * alguien estaba cargando un hechizo.
     */
    private static void drawChannel(Player player, Spell spell, int elapsed, int totalTicks) {

        double progress = Math.min(1.0, elapsed / (double) totalTicks);
        Location base = player.getLocation();
        Particle.DustOptions dust = new Particle.DustOptions(colorOf(spell.color()), 1.3f);
        double radius = 1.6 - 1.1 * progress;
        double height = 0.1 + 2.0 * progress;

        for (int strand = 0; strand < CHANNEL_STRANDS; strand++) {
            double angle = elapsed * 0.35 + strand * (2 * Math.PI / CHANNEL_STRANDS);
            Location point = base.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            player.getWorld().spawnParticle(Particle.DUST, point, 2, 0.03, 0.03, 0.03, 0, dust, true);
            player.getWorld().spawnParticle(Particle.ENCHANT, point, 1, 0, 0, 0, 0.4, null, true);
        }

        if (elapsed % 10 == 1) {
            player.getWorld().playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, (float) (0.6 + progress));
        }
    }

    /** El {@code color} del hechizo (nombre de color de chat) como color de partícula. */
    private static Color colorOf(String raw) {
        NamedTextColor named = raw == null ? null : NamedTextColor.NAMES.value(raw.trim().toLowerCase(Locale.ROOT));
        int rgb = (named != null ? named : NamedTextColor.LIGHT_PURPLE).value();
        return Color.fromRGB(rgb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player) || !isChanneling(player.getUniqueId())) {
            return;
        }

        lang.send(player, "channel.cancelled_damage");
        cancelChannel(player.getUniqueId());
    }

    private void cancelChannel(UUID uuid) {

        BukkitTask task = channeling.remove(uuid);

        if (task != null) {
            task.cancel();
        }
    }

}
