package com.sack.rpgroll.items.requirement;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.items.core.ItemRequirements;
import com.sack.rpgroll.common.character.Characters;
import com.sack.rpgroll.common.character.RPGCharacters;
import com.sack.rpgroll.common.integration.VaultEconomy;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Valida los {@link ItemRequirements} de un ítem contra el estado actual de un jugador. */
public class ItemRequirementChecker {

    private final LangManager langManager;

    public ItemRequirementChecker(LangManager langManager) {
        this.langManager = langManager;
    }

    public List<String> check(Player player, ItemRequirements req) {

        List<String> reasons = new ArrayList<>();

        // Sin el core de RPGRoll no hay personajes: los requisitos de personaje
        // no aplican y el ítem se usa como cualquier otro.
        Characters.get().ifPresent(characters -> checkCharacter(player, req, characters, reasons));

        if (req.permission() != null && !player.hasPermission(req.permission())) {
            reasons.add(langManager.raw("requirement.permission"));
        }

        if (req.money() > 0 && VaultEconomy.isAvailable()) {
            double balance = VaultEconomy.get().map(eco -> eco.getBalance(player)).orElse(0.0);
            if (balance < req.money()) {
                reasons.add(langManager.raw("requirement.money", "amount", req.money()));
            }
        }

        return reasons;
    }

    private void checkCharacter(Player player, ItemRequirements req, RPGCharacters characters,
            List<String> reasons) {

        UUID uuid = player.getUniqueId();

        if (req.level() > 0) {
            int level = characters.level(uuid);
            if (level < req.level()) {
                reasons.add(langManager.raw("requirement.level", "level", req.level(), "current", level));
            }
        }

        if (req.race() != null && !req.race().equalsIgnoreCase(characters.race(uuid).orElse(null))) {
            reasons.add(langManager.raw("requirement.race", "race", req.race()));
        }

        if (req.playerClass() != null
                && !req.playerClass().equalsIgnoreCase(characters.playerClass(uuid).orElse(null))) {
            reasons.add(langManager.raw("requirement.class", "class", req.playerClass()));
        }

        if (req.profession() != null && !characters.hasJob(uuid, req.profession())) {
            reasons.add(langManager.raw("requirement.profession", "profession", req.profession()));
        }

        if (req.trait() != null && !characters.hasTrait(uuid, req.trait())) {
            reasons.add(langManager.raw("requirement.trait", "trait", req.trait()));
        }

        if (req.skill() != null && !characters.hasSkill(uuid, req.skill())) {
            reasons.add(langManager.raw("requirement.skill", "skill", req.skill()));
        }
    }

}
