package com.sack.rpgroll.quests.integration;

import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestManager;
import com.sack.rpgroll.quests.core.QuestStage;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.quests.player.QuestPlayerStateManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;

/**
 * Expansión de PlaceholderAPI de Misiones: %rpgrollquests_&lt;placeholder&gt;%.
 */
public class QuestsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final QuestPlayerStateManager stateManager;
    private final QuestManager questManager;

    public QuestsPlaceholders(Plugin plugin, QuestPlayerStateManager stateManager, QuestManager questManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.questManager = questManager;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollquests";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (player == null) {
            return "";
        }

        QuestPlayerState state = stateManager.getOrLoad(player);
        String key = params.toLowerCase(Locale.ROOT);

        switch (key) {
            case "active_count":
                return String.valueOf(state.allActive().size());
            case "completed_count":
                return String.valueOf(state.totalCompletedCount());
            default:
                break;
        }

        if (key.startsWith("has_completed_")) {
            String questId = key.substring("has_completed_".length());
            return !questId.isBlank() && state.hasCompleted(questId) ? "si" : "no";
        }

        if (key.startsWith("is_active_")) {
            String questId = key.substring("is_active_".length());
            return !questId.isBlank() && state.isActive(questId) ? "si" : "no";
        }

        if (key.startsWith("active_") && key.endsWith("_stage")) {
            String questId = key.substring("active_".length(), key.length() - "_stage".length());
            return questId.isBlank() ? "" : resolveStage(state, questId);
        }

        return "";
    }

    private String resolveStage(QuestPlayerState state, String questId) {

        Optional<ActiveQuestProgress> progressOpt = state.getActive(questId);
        Optional<Quest> questOpt = questManager.get(questId);

        if (progressOpt.isEmpty() || questOpt.isEmpty()) {
            return "-";
        }

        return questOpt.get().stageAt(progressOpt.get().stageIndex())
                .map(QuestStage::id)
                .orElse("-");
    }

}
