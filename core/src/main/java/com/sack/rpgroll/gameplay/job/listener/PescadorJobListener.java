package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Otorga recompensas de trabajo "pescador" al pescar con éxito.
 * No requiere anti-farm — no hay forma de "recolocar" una captura de pesca.
 */
public class PescadorJobListener implements Listener {

    private static final String JOB_ID = "pescador";

    private final JobRewardService rewardService;

    public PescadorJobListener(JobRewardService rewardService) {
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        if (!(event.getCaught() instanceof Item caughtItem)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack caughtStack = caughtItem.getItemStack();
        String target = caughtStack.getType().name();

        rewardService.reward(player, JOB_ID, target);
    }

}