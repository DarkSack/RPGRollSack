package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.ascension.engine.ProgressService;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;
import com.sack.rpgroll.mobs.api.MobDeathEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Solo se registra si RPGRoll-Mobs está habilitado: referencia sus clases.
 * Un mob de RPGRoll cuenta dos veces, como KILL_MOB por su id y como
 * KILL_ENTITY por su tipo base, porque es las dos cosas.
 */
public class MobProgressListener implements Listener {

    private final ProgressService progress;

    public MobProgressListener(ProgressService progress) {
        this.progress = progress;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(MobDeathEvent event) {

        if (event.getKiller() == null) {
            return;
        }

        progress.handle(event.getKiller(),
                new ProgressEvent(TriggerType.KILL_MOB, event.getDefinition().id(), null));
    }

}
