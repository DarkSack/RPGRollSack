package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Otorga recompensas de trabajo "minero" al romper bloques.
 * <p>
 * PENDIENTE: protección anti-farm (evitar XP/dinero infinito colocando y
 * rompiendo el mismo bloque repetidamente). El enfoque inicial vía
 * PersistentDataContainer en el Block no es viable — Block no implementa
 * PersistentDataHolder (solo TileState lo hace, para bloques con block
 * entity como cofres/hornos, no para piedra/minerales). Requiere una
 * solución dedicada (tracking en BD o en memoria) en un PR aparte.
 */
public class MinerJobListener implements Listener {

    private static final String JOB_ID = "minero";

    private final JobRewardService rewardService;

    public MinerJobListener(JobRewardService rewardService) {
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();
        String target = block.getType().name();

        rewardService.reward(player, JOB_ID, target);
    }

}