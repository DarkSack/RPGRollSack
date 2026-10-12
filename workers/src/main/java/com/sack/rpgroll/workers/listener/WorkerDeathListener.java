package com.sack.rpgroll.workers.listener;

import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTransformEvent;

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

    /**
     * Un worker no se transforma (rayo, zombi) ni cruza portales: su ficha se quedaba apuntando a
     * un mob que ya no existe, o a uno en otro mundo lejos de su hogar y su almacén.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (workerManager.isTracked(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(EntityPortalEvent event) {
        if (workerManager.isTracked(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {

        // Solo el mob del worker: resolve() también reconoce su maniquí, y un /kill al maniquí no lo mata.
        if (!workerManager.isTracked(event.getEntity())) {
            return;
        }

        Worker worker = workerManager.get(event.getEntity().getUniqueId()).orElse(null);

        if (worker == null) {
            return;
        }

        EntityReskinService.remove(event.getEntity());
        workerManager.remove(worker.id());
    }

}
