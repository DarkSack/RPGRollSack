package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.api.event.CharacterSelectionEvent;
import com.sack.rpgroll.api.event.PlayerSelectClassEvent;
import com.sack.rpgroll.api.event.PlayerSelectRaceEvent;
import com.sack.rpgroll.ascension.deferred.SecretTargetType;
import com.sack.rpgroll.ascension.engine.SecretEngine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Impide elegir una raza o clase secreta que el jugador aún no ha desbloqueado. */
public class SecretSelectionListener implements Listener {

    private final SecretEngine secretEngine;

    public SecretSelectionListener(SecretEngine secretEngine) {
        this.secretEngine = secretEngine;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRace(PlayerSelectRaceEvent event) {
        gate(event, SecretTargetType.RACE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClass(PlayerSelectClassEvent event) {
        gate(event, SecretTargetType.CLASS);
    }

    private void gate(CharacterSelectionEvent event, SecretTargetType type) {
        secretEngine.lockReasons(event.getPlayer(), type, event.getSelectedId()).forEach(event::deny);
    }

}
