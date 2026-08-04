package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonObjectiveType;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.engine.DungeonSession;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Progresa objetivos de sala que no se resuelven solos vía oleadas/jefes:
 * KILL_ENTITY (entidades sueltas, fuera del sistema de oleadas),
 * COLLECT_ITEM, DESTROY_BLOCK y ACTIVATE_MECHANISM (interactuar con un
 * bloque en coordenadas exactas — la forma en que este motor resuelve
 * "puzzles" sin un editor visual dedicado).
 */
public class DungeonObjectiveListener implements Listener {

    private final DungeonEngine engine;

    public DungeonObjectiveListener(DungeonEngine engine) {
        this.engine = engine;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        withSession(killer, (session, definition) -> engine.progressObjective(session, definition,
                DungeonObjectiveType.KILL_ENTITY, Map.of("entity", event.getEntityType().name()), 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        withSession(player, (session, definition) -> engine.progressObjective(session, definition,
                DungeonObjectiveType.COLLECT_ITEM,
                Map.of("material", event.getItem().getItemStack().getType().name()),
                event.getItem().getItemStack().getAmount()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        withSession(event.getPlayer(), (session, definition) -> engine.progressObjective(session, definition,
                DungeonObjectiveType.DESTROY_BLOCK, Map.of("material", event.getBlock().getType().name()), 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();

        withSession(event.getPlayer(), (session, definition) -> engine.progressObjective(session, definition,
                DungeonObjectiveType.ACTIVATE_MECHANISM,
                Map.of("x", String.valueOf(block.getX()), "y", String.valueOf(block.getY()),
                        "z", String.valueOf(block.getZ())),
                1));
    }

    private void withSession(Player player, ObjectiveAction action) {

        Optional<DungeonSession> sessionOpt = engine.findSessionByPlayer(player.getUniqueId());
        if (sessionOpt.isEmpty()) {
            return;
        }

        DungeonSession session = sessionOpt.get();
        DungeonDefinition definition = engine.getDungeonManager().get(session.dungeonId()).orElse(null);

        if (definition != null) {
            action.run(session, definition);
        }
    }

    @FunctionalInterface
    private interface ObjectiveAction {
        void run(DungeonSession session, DungeonDefinition definition);
    }

}
