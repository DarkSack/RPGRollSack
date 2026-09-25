package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Puente blando hacia :core para otorgar xp de personaje al completar una receta. */
public final class CharacterXpBridge {

    private CharacterXpBridge() {
    }

    public static void grant(UUID playerId, double amount) {

        if (amount <= 0 || !RPGRollAPI.isReady()) {
            return;
        }

        int experience = (int) Math.round(amount);
        Player online = Bukkit.getPlayer(playerId);

        if (online != null) {
            experience = RPGRollAPI.get().getExperienceBonusService().boost(online, experience);
        }

        int granted = experience;
        RPGRollAPI.get().getPlayer(playerId).ifPresent(player -> {
            RPGPlayer updated = player.addExperience(granted);
            RPGRollAPI.get().getPlayerManager().savePlayer(updated);
        });
    }

}
