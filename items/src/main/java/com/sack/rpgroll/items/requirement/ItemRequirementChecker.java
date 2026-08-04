package com.sack.rpgroll.items.requirement;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.items.core.ItemRequirements;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Valida los {@link ItemRequirements} de un ítem contra el estado actual de un jugador. */
public class ItemRequirementChecker {

    public List<String> check(Player player, ItemRequirements req) {

        List<String> reasons = new ArrayList<>();

        RPGPlayer rpgPlayer = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        if (req.level() > 0) {
            int level = rpgPlayer != null ? rpgPlayer.getLevel() : 0;
            if (level < req.level()) {
                reasons.add("Necesitás nivel " + req.level() + " (tenés " + level + ").");
            }
        }

        if (req.race() != null && (rpgPlayer == null || !req.race().equalsIgnoreCase(rpgPlayer.getRace()))) {
            reasons.add("Necesitás ser de la raza: " + req.race());
        }

        if (req.playerClass() != null
                && (rpgPlayer == null || !req.playerClass().equalsIgnoreCase(rpgPlayer.getPlayerClass()))) {
            reasons.add("Necesitás ser de la clase: " + req.playerClass());
        }

        if (req.profession() != null
                && (rpgPlayer == null || !rpgPlayer.getJobs().hasJob(req.profession()))) {
            reasons.add("Necesitás la profesión: " + req.profession());
        }

        if (req.trait() != null && (rpgPlayer == null || !rpgPlayer.getTraits().hasTrait(req.trait()))) {
            reasons.add("Necesitás el trait: " + req.trait());
        }

        if (req.skill() != null && (rpgPlayer == null || !rpgPlayer.getSkills().hasSkill(req.skill()))) {
            reasons.add("Necesitás la habilidad: " + req.skill());
        }

        if (req.permission() != null && !player.hasPermission(req.permission())) {
            reasons.add("No tenés el permiso requerido.");
        }

        if (req.money() > 0 && RPGRollAPI.isReady()) {
            var economyProvider = RPGRollAPI.get().getEconomyProvider();
            if (economyProvider.isAvailable()) {
                double balance = economyProvider.getEconomy().map(eco -> eco.getBalance(player)).orElse(0.0);
                if (balance < req.money()) {
                    reasons.add("Necesitás al menos " + req.money() + " de dinero.");
                }
            }
        }

        return reasons;
    }

}
