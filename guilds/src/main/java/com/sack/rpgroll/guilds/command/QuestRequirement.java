package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.quests.QuestsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Requisito "haber completado una misión" para fundar una guild. Vive aparte
 * porque es la única clase que toca RPGRoll-Quests: solo se carga cuando ese
 * plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class QuestRequirement {

    private QuestRequirement() {
    }

    static boolean hasCompleted(Player player, String questId) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Quests") instanceof QuestsPlugin quests)) {
            return true;
        }

        return quests.getQuestEngine().getStateManager().getOrLoad(player).hasCompleted(questId);
    }

}
