package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.mobs.api.MobDeathEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Traduce la muerte de un mob de RPGRoll-Mobs en progreso de oleada/jefe si pertenece a una corrida activa. */
public class DungeonMobDeathListener implements Listener {

    private final DungeonEngine engine;

    public DungeonMobDeathListener(DungeonEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onMobDeath(MobDeathEvent event) {
        engine.onMobDeath(event.getEntity().getUniqueId());
    }

}
