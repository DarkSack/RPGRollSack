package com.sack.rpgroll.guilds.buff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Aplica los bonos de daño/defensa de equipo y guild al combate real. */
public class BuffCombatListener implements Listener {

    private final BuffCalculator buffCalculator;

    public BuffCombatListener(BuffCalculator buffCalculator) {
        this.buffCalculator = buffCalculator;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        double damage = event.getDamage();

        if (event.getDamager() instanceof Player attacker) {
            damage *= 1 + buffCalculator.damageDealtBonus(attacker.getUniqueId());
        }

        if (event.getEntity() instanceof Player victim) {
            damage *= Math.max(0, 1 - buffCalculator.damageTakenReduction(victim.getUniqueId()));
        }

        event.setDamage(damage);
    }

}
