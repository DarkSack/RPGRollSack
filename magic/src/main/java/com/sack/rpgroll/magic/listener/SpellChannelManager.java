package com.sack.rpgroll.magic.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCatalyst;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
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

                if (elapsed >= totalTicks) {

                    channeling.remove(player.getUniqueId());
                    CastResult result = engine.cast(spell, player, spellbook, catalyst);

                    if (!result.success()) {
                        result.reasons().forEach(reason -> player.sendMessage(
                                Component.text("✘ " + reason, NamedTextColor.RED)));
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        channeling.put(player.getUniqueId(), task);
    }

    @EventHandler
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
