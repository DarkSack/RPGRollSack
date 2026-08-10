package com.sack.rpgroll.extras.consumption;

import com.sack.rpgroll.extras.stat.StatEngine;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Acciones nativas del jugador que pueden consumir stats (sección 3):
 * sprint (al empezar a correr, no continuo — para drenado continuo usar
 * una regla de regeneración condicional con {@code condition: sprinting}
 * y monto negativo, sección 4), salto, ataque, minado. Pesca/farming/
 * habilidades de otros addons no se detectan acá — le corresponde a CADA
 * addon reportar su propia acción vía {@code ExtrasAPI.get().stats().consumeAll(...)}.
 */
public class ConsumptionListener implements Listener {

    private final StatEngine statEngine;

    public ConsumptionListener(StatEngine statEngine) {
        this.statEngine = statEngine;
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {

        if (event.isSprinting()) {
            statEngine.consumeAll(event.getPlayer(), "sprint");
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        statEngine.consumeAll(event.getPlayer(), "jump");
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player attacker) {
            statEngine.consumeAll(attacker, "attack");
        }
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        statEngine.consumeAll(event.getPlayer(), "mining");
    }

}
