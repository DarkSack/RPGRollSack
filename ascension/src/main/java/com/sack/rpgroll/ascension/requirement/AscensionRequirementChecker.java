package com.sack.rpgroll.ascension.requirement;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.quests.QuestsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Valida {@link AscensionRequirements} contra el jugador y su estado de Ascension. */
public class AscensionRequirementChecker {

    private final LangManager lang;

    public AscensionRequirementChecker(LangManager lang) {
        this.lang = lang;
    }

    public List<String> check(Player player, AscensionRequirements req, AscensionPlayerState state) {

        List<String> reasons = new ArrayList<>();

        RPGPlayer rpgPlayer = RPGRollAPI.isReady() ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        if (req.level() > 0) {
            int level = rpgPlayer != null ? rpgPlayer.getLevel() : 0;
            if (level < req.level()) {
                reasons.add(lang.raw("requirement.level", "required", req.level(), "current", level));
            }
        }

        if (req.prestige() > 0 && state.getPrestigeCount() < req.prestige()) {
            reasons.add(lang.raw("requirement.prestige", "required", req.prestige(), "current",
                    state.getPrestigeCount()));
        }

        if (req.trait() != null && (rpgPlayer == null || !rpgPlayer.getTraits().hasTrait(req.trait()))) {
            reasons.add(lang.raw("requirement.trait", "trait", req.trait()));
        }

        for (var entry : req.reputation().entrySet()) {
            if (state.getReputation(entry.getKey()) < entry.getValue()) {
                reasons.add(lang.raw("requirement.reputation", "amount", entry.getValue(), "faction",
                        entry.getKey()));
            }
        }

        // Nota: "completed-quests" se valida acá solo si RPGRoll-Quests está presente;
        // sin esa integración, un requisito de quest completada nunca bloquea.
        if (!req.completedQuests().isEmpty()) {
            reasons.addAll(checkCompletedQuests(player, req.completedQuests()));
        }

        return reasons;
    }

    private List<String> checkCompletedQuests(Player player, List<String> questIds) {

        List<String> reasons = new ArrayList<>();
        var questsPlugin = Bukkit.getPluginManager().getPlugin("RPGRoll-Quests");

        if (!(questsPlugin instanceof QuestsPlugin quests)) {
            return reasons;
        }

        var state = quests.getQuestEngine().getStateManager().getOrLoad(player);

        for (String questId : questIds) {
            if (!state.hasCompleted(questId)) {
                reasons.add(lang.raw("requirement.completed_quest", "quest", questId));
            }
        }

        return reasons;
    }

}
