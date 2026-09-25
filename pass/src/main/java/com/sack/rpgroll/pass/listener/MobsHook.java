package com.sack.rpgroll.pass.listener;

import com.sack.rpgroll.mobs.api.MobDeathEvent;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Solo se registra con RPGRoll-Mobs activo: sus clases no existen sin él. */
public class MobsHook implements Listener {

    private final MissionService missions;

    public MobsHook(MissionService missions) {
        this.missions = missions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(MobDeathEvent event) {

        if (event.getKiller() != null) {
            missions.progress(event.getKiller(), MissionType.KILL_RPG_MOB, event.getDefinition().id(), 1);
        }
    }

}
