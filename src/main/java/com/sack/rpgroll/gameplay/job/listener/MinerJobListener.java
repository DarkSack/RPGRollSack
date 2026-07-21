package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import com.sack.rpgroll.gameplay.job.PlacedBlockTracker;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Otorga recompensas de trabajo "minero" al romper bloques.
 * <p>
 * Protección anti-farm: los bloques colocados por un jugador se rastrean
 * en SQLite (PlacedBlockTracker). Al romperlos, no se paga recompensa —
 * evita el ciclo de colocar/romper el mismo bloque para XP/dinero infinito.
 */
public class MinerJobListener implements Listener {

    private static final String JOB_ID = "minero";

    private final JobRewardService rewardService;
    private final PlacedBlockTracker placedBlockTracker;

    public MinerJobListener(JobRewardService rewardService, PlacedBlockTracker placedBlockTracker) {
        this.rewardService = rewardService;
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        placedBlockTracker.markPlaced(event.getBlock());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (placedBlockTracker.isPlayerPlacedAndClear(block)) {
            return;
        }

        Player player = event.getPlayer();
        String target = block.getType().name();

        rewardService.reward(player, JOB_ID, target);
    }

}