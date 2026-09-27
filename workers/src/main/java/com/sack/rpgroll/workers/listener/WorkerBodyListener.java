package com.sack.rpgroll.workers.listener;

import com.sack.rpgroll.workers.core.skin.WorkerBodyService;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * El maniquí de un worker con skin es solo lo que se ve: los golpes, el daño y la muerte son
 * cosa del mob invisible que lo mueve (ver {@link WorkerBodyService}).
 */
public class WorkerBodyListener implements Listener {

    private final WorkerBodyService bodies;

    public WorkerBodyListener(WorkerBodyService bodies) {
        this.bodies = bodies;
    }

    /**
     * Pegarle al maniquí es pegarle al worker: el golpe pasa al mob, con el arma, los
     * encantamientos y las protecciones de siempre.
     */
    @EventHandler(ignoreCancelled = true)
    public void onAttackBody(PrePlayerAttackEntityEvent event) {

        LivingEntity driver = bodies.driverOf(event.getAttacked());

        if (driver == null) {
            return;
        }

        event.setCancelled(true);

        if (event.willAttack()) {
            event.getPlayer().attack(driver);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDriverDamaged(EntityDamageEvent event) {

        if (event.getEntity() instanceof LivingEntity living && event.getFinalDamage() > 0) {
            bodies.hurt(living);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {

        // Un maniquí matado a mano (/kill): sin botín ni experiencia; el tick lo vuelve a crear.
        if (bodies.isBody(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }

        bodies.died(event.getEntity());
    }

}
