package com.sack.rpgroll.dungeons.integration;

import com.sack.rpgroll.quests.QuestsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Integración blanda con RPGRoll-Quests: loot de tipo QUEST inicia una misión si el addon está presente. */
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
