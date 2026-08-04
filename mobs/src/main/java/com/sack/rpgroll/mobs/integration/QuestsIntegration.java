package com.sack.rpgroll.mobs.integration;

import com.sack.rpgroll.quests.QuestsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Integración blanda con RPGRoll-Quests: permite que el loot de un mob
 * inicie una quest directamente. Softdepend — se resuelve en runtime.
 */
public final class QuestsIntegration {

    private QuestsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Quests") instanceof QuestsPlugin;
    }

    public static void startQuest(Player player, String questId) {

        if (!isAvailable()) {
            return;
        }

        var plugin = (QuestsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Quests");

        plugin.getQuestManager().get(questId)
                .ifPresent(quest -> plugin.getQuestEngine().startQuest(player, quest));
    }

}
