package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonModifierType;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.engine.DungeonSession;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Puentea el combate vanilla con {@link DungeonEngine}: registra
 * contribución de daño de jugadores contra mobs de la corrida (para loot
 * PER_CONTRIBUTION y el ranking), aplica el multiplicador de daño de la
 * dificultad cuando un mob de la corrida golpea a un jugador, bloquea
 * curación si la dificultad tiene el modificador NO_HEALING, y traduce
 * la muerte de un jugador dentro de una dungeon en {@code onPlayerDeath}.
 */
public class DungeonCombatListener implements Listener {

    private final DungeonEngine engine;

    public DungeonCombatListener(DungeonEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        // Jugador golpeando a un mob de la corrida → contribución de daño.
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity target) {
            engine.findSessionByEntity(target.getUniqueId())
                    .ifPresent(session -> session.addDamageContribution(player.getUniqueId(), event.getFinalDamage()));
        }

        // Mob de la corrida golpeando a un jugador → multiplicador de dificultad.
        if (event.getDamager() instanceof LivingEntity damager && event.getEntity() instanceof Player) {
            double multiplier = engine.outgoingDamageMultiplier(damager.getUniqueId());
            if (multiplier != 1.0) {
                event.setDamage(event.getDamage() * multiplier);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        engine.findSessionByPlayer(player.getUniqueId()).ifPresent(session -> {
            if (session.difficulty().hasModifier(DungeonModifierType.NO_HEALING)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getPlayer();

        engine.findSessionByPlayer(player.getUniqueId()).ifPresent(session -> {

            DungeonDefinition definition = engine.getDungeonManager().get(session.dungeonId()).orElse(null);
            if (definition == null) {
                return;
            }

            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            engine.onPlayerDeath(player, session, definition);
        });
    }

}
