package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Otorga recompensas de trabajo "granjero" al cosechar cultivos maduros.
 * <p>
 * Protección anti-farm: solo se paga si el cultivo está en su edad máxima
 * (completamente maduro). Romper un cultivo inmaduro para replantar rápido
 * no otorga nada — desincentiva el farmeo acelerado de cultivos jóvenes.
 */
public class GranjeroJobListener implements Listener {

    private static final String JOB_ID = "granjero";

    private final JobRewardService rewardService;

    public GranjeroJobListener(JobRewardService rewardService) {
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        BlockData blockData = block.getBlockData();

        if (!(blockData instanceof Ageable ageable)) {
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        Player player = event.getPlayer();
        String target = block.getType().name();

        rewardService.reward(player, JOB_ID, target);
    }

}