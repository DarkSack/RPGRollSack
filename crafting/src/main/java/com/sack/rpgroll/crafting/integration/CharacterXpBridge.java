package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.common.character.Characters;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Puente blando hacia el core (vía RPGRoll-Lib) para otorgar xp de personaje al completar una receta. */
public final class CharacterXpBridge {

    private CharacterXpBridge() {
    }

    public static void grant(UUID playerId, double amount) {

        if (amount <= 0) {
            return;
        }

        Characters.get().ifPresent(characters -> {
            int experience = (int) Math.round(amount);
            Player online = Bukkit.getPlayer(playerId);

            if (online != null) {
                experience = characters.boostExperience(online, experience);
            }

            characters.addExperience(playerId, experience);
        });
    }

}
