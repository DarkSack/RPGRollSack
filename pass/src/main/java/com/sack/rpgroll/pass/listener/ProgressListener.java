package com.sack.rpgroll.pass.listener;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.gameplay.event.LevelUpEvent;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

/** Misiones que salen de eventos vanilla y del core de RPGRoll. */
public class ProgressListener implements Listener {

    private final MissionService missions;

    public ProgressListener(MissionService missions) {
        this.missions = missions;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {

        Player killer = event.getEntity().getKiller();

        if (killer != null && !(event.getEntity() instanceof Player)) {
            missions.progress(killer, MissionType.KILL_MOB, event.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        // Los bloques que puso un jugador no cuentan: si no, basta con poner y quitar.
        if (RPGRollAPI.isReady() && RPGRollAPI.get().getPlacedBlockTracker().isPlayerPlaced(event.getBlock())) {
            return;
        }

        missions.progress(event.getPlayer(), MissionType.BREAK_BLOCK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            missions.progress(event.getPlayer(), MissionType.FISH, "", 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(LevelUpEvent event) {
        missions.progress(event.getPlayer(), MissionType.LEVEL_UP, "", 1);
    }

}
