package com.sack.rpgroll.traps.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.traps.core.TrapBlockConfig;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.integration.ItemsIntegration;
import com.sack.rpgroll.traps.location.PlacedTrap;
import com.sack.rpgroll.traps.location.PlacedTrapManager;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Optional;

/**
 * Protege el bloque físico de una trampa (puertas reforzadas, muros con
 * llave): sin este listener no habría forma de que {@code block.breakable}/
 * {@code requiredItemId}/{@code explosionImmune}/{@code pistonImmune}
 * tuvieran efecto real — no hay precedente en el repo para esto, se escribe
 * desde cero (ver plan).
 */
public class TrapBlockProtectionListener implements Listener {

    private final TrapManager trapManager;
    private final PlacedTrapManager placedTrapManager;
    private final LangManager lang;

    public TrapBlockProtectionListener(TrapManager trapManager, PlacedTrapManager placedTrapManager,
            LangManager lang) {
        this.trapManager = trapManager;
        this.placedTrapManager = placedTrapManager;
        this.lang = lang;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Optional<PlacedTrap> placedOpt = placedTrapManager.findAt(event.getBlock().getLocation());
        if (placedOpt.isEmpty()) {
            return;
        }

        Optional<TrapDefinition> definitionOpt = trapManager.get(placedOpt.get().trapId());
        if (definitionOpt.isEmpty()) {
            return;
        }

        TrapBlockConfig block = definitionOpt.get().block();
        if (!block.isConfigured()) {
            return;
        }

        if (!block.breakable()) {
            event.setCancelled(true);
            lang.send(event.getPlayer(), "protection.not_breakable");
            return;
        }

        if (block.requiresKey() && !ItemsIntegration.hasKey(event.getPlayer(), block.requiredItemId())) {
            event.setCancelled(true);
            lang.send(event.getPlayer(), "protection.missing_key");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isExplosionImmune);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isExplosionImmune);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (anyPistonImmune(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (anyPistonImmune(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    private boolean isExplosionImmune(Block block) {
        return placedTrapManager.findAllAt(block.getLocation()).stream()
                .anyMatch(placed -> trapManager.get(placed.trapId())
                        .map(TrapDefinition::block)
                        .map(TrapBlockConfig::explosionImmune)
                        .orElse(false));
    }

    private boolean anyPistonImmune(List<Block> blocks) {
        for (Block block : blocks) {
            boolean immune = placedTrapManager.findAllAt(block.getLocation()).stream()
                    .anyMatch(placed -> trapManager.get(placed.trapId())
                            .map(TrapDefinition::block)
                            .map(TrapBlockConfig::pistonImmune)
                            .orElse(false));
            if (immune) {
                return true;
            }
        }
        return false;
    }

}
