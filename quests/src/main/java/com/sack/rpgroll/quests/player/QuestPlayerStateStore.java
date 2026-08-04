package com.sack.rpgroll.quests.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persiste el {@link QuestPlayerState} de cada jugador en su propio archivo
 * plugins/RPGRoll-Quests/playerdata/&lt;uuid&gt;.yml — este addon es
 * autocontenido y no depende de la base de datos SQLite de :core para esto.
 */
public class QuestPlayerStateStore {

    private final Plugin plugin;
    private final File folder;

    public QuestPlayerStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public QuestPlayerState load(UUID uuid) {

        QuestPlayerState state = new QuestPlayerState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadActive(config, state);
        loadCompleted(config, state);
        loadFailed(config, state);

        state.addTimeSpent(config.getLong("total-time-spent-millis", 0));
        state.setTotalCompletedCount(config.getInt("total-completed-count", 0));

        return state;
    }

    private void loadActive(YamlConfiguration config, QuestPlayerState state) {

        var section = config.getConfigurationSection("active");
        if (section == null) {
            return;
        }

        for (String questId : section.getKeys(false)) {

            int stageIndex = section.getInt(questId + ".stage-index", 0);
            long startedAt = section.getLong(questId + ".started-at", System.currentTimeMillis());

            ActiveQuestProgress progress = new ActiveQuestProgress(questId, stageIndex, startedAt);
            progress.restoreStageStartedAt(section.getLong(questId + ".stage-started-at", startedAt));

            var progressSection = section.getConfigurationSection(questId + ".objective-progress");
            if (progressSection != null) {

                Map<Integer, Integer> restored = new HashMap<>();

                for (String key : progressSection.getKeys(false)) {
                    try {
                        restored.put(Integer.parseInt(key), progressSection.getInt(key));
                    } catch (NumberFormatException ignored) {
                    }
                }

                progress.restoreProgress(restored);
            }

            state.allActive().put(questId, progress);
        }
    }

    private void loadCompleted(YamlConfiguration config, QuestPlayerState state) {

        var section = config.getConfigurationSection("completed");
        if (section == null) {
            return;
        }

        for (String questId : section.getKeys(false)) {
            state.allCompleted().put(questId, section.getLong(questId));
        }
    }

    private void loadFailed(YamlConfiguration config, QuestPlayerState state) {
        state.allFailed().addAll(config.getStringList("failed"));
    }

    public void save(QuestPlayerState state) {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : state.allActive().entrySet()) {

            String questId = entry.getKey();
            ActiveQuestProgress progress = entry.getValue();

            config.set("active." + questId + ".stage-index", progress.stageIndex());
            config.set("active." + questId + ".started-at", progress.startedAtMillis());
            config.set("active." + questId + ".stage-started-at", progress.stageStartedAtMillis());

            for (var objectiveEntry : progress.allProgress().entrySet()) {
                config.set("active." + questId + ".objective-progress." + objectiveEntry.getKey(),
                        objectiveEntry.getValue());
            }
        }

        for (var entry : state.allCompleted().entrySet()) {
            config.set("completed." + entry.getKey(), entry.getValue());
        }

        config.set("failed", state.allFailed().stream().toList());
        config.set("total-time-spent-millis", state.totalTimeSpentMillis());
        config.set("total-completed-count", state.totalCompletedCount());

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando progreso de quests de " + state.uuid() + ": "
                    + e.getMessage());
        }
    }

}
