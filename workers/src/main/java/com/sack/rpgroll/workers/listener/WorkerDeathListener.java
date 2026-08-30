package com.sack.rpgroll.workers.listener;

import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Limpia el registro de un worker (y su passenger de reskin, si tenía) al
 * morir su entidad — sin esto, tanto {@link WorkerManager} como el
 * {@code ItemDisplay} del reskin quedan huérfanos indefinidamente (mismo
 * hueco que se encontró y corrigió en RPGRoll-Ranching).
 */
public class WorkerDeathListener implements Listener {

    private final WorkerManager workerManager;

    public WorkerDeathListener(WorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {

        Worker worker = workerManager.resolve(event.getEntity()).orElse(null);

        if (worker == null) {
            return;
        }

        EntityReskinService.remove(event.getEntity());
        workerManager.remove(worker.id());
    }

}
