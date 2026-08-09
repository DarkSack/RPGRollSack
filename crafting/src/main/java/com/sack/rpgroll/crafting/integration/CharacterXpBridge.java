package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.player.RPGPlayer;

import java.util.UUID;

/** Puente blando hacia :core para otorgar xp de personaje al completar una receta. */
public final class CharacterXpBridge {

    private CharacterXpBridge() {
    }

    public static void grant(UUID playerId, double amount) {

        if (amount <= 0 || !RPGRollAPI.isReady()) {
            return;
        }

        RPGRollAPI.get().getPlayer(playerId).ifPresent(player -> {
            RPGPlayer updated = player.addExperience((int) Math.round(amount));
            RPGRollAPI.get().getPlayerManager().savePlayer(updated);
        });
    }

}
