package com.sack.rpgroll.extras.activity;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Marca a atacante y víctima como "en combate" (ventana de tiempo) para las reglas de regeneración condicional. */
public class CombatTagListener implements Listener {

    private final ActivityStateResolver activityStateResolver;

    public CombatTagListener(ActivityStateResolver activityStateResolver) {
        this.activityStateResolver = activityStateResolver;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof Player victim) {
            activityStateResolver.markCombat(victim);
        }

        if (event.getDamager() instanceof Player attacker) {
            activityStateResolver.markCombat(attacker);
        }
    }

}
