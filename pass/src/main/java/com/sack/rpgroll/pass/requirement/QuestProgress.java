package com.sack.rpgroll.pass.requirement;

import com.sack.rpgroll.quests.QuestsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * La única clase del requisito {@code quests} que toca RPGRoll-Quests: solo
 * se carga si ese plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class QuestProgress {

    private QuestProgress() {
    }

    static boolean hasCompleted(Player player, String questId) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Quests") instanceof QuestsPlugin quests)) {
            return true;
        }

        return quests.getQuestEngine().getStateManager().getOrLoad(player).hasCompleted(questId);
    }

}
