package com.sack.rpgroll.quests.engine;

import com.sack.rpgroll.quests.core.Quest;
import com.sack.rpgroll.quests.core.QuestObjective;
import com.sack.rpgroll.quests.core.QuestStage;
import com.sack.rpgroll.quests.player.ActiveQuestProgress;
import com.sack.rpgroll.quests.player.QuestPlayerState;
import com.sack.rpgroll.quests.region.RegionManager;
import com.sack.rpgroll.quests.util.DurationParser;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Corre cada segundo: revisa los objetivos que no dependen de un evento
 * externo puntual (WAIT cuenta tiempo, REACH_LOCATION/DISCOVER_REGION
 * dependen de la posición actual del jugador) y hace avanzar el stage de
 * cualquier quest que solo esté bloqueada por una condición (ej. subir de
 * nivel mientras esperaba con los objetivos ya completos).
 */
public class QuestPollingTask implements Runnable {

    private final QuestEngine engine;
    private final RegionManager regionManager;

    public QuestPollingTask(QuestEngine engine, RegionManager regionManager) {
        this.engine = engine;
        this.regionManager = regionManager;
    }

    @Override
    public void run() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {

        QuestPlayerState state = engine.getStateManager().getOrLoad(player);

        for (ActiveQuestProgress progress : List.copyOf(state.allActive().values())) {

            Optional<Quest> questOpt = engine.getQuestManager().get(progress.questId());
            if (questOpt.isEmpty()) {
                continue;
            }

            Quest quest = questOpt.get();
            Optional<QuestStage> stageOpt = quest.stageAt(progress.stageIndex());
            if (stageOpt.isEmpty()) {
                continue;
            }

            QuestStage stage = stageOpt.get();
            List<QuestObjective> objectives = stage.objectives();

            for (int i = 0; i < objectives.size(); i++) {
                checkObjective(player, quest, stage, progress, objectives.get(i), i);
            }

            engine.maybeAdvanceStage(player, quest, stage, progress);
        }
    }

    private void checkObjective(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            QuestObjective objective, int index) {

        if (progress.getProgress(index) >= objective.amount()) {
            return;
        }

        switch (objective.type().toUpperCase()) {
            case "WAIT" -> checkWait(player, quest, stage, progress, objective, index);
            case "REACH_LOCATION" -> checkReachLocation(player, quest, stage, progress, objective, index);
            case "DISCOVER_REGION" -> checkDiscoverRegion(player, quest, stage, progress, objective, index);
            default -> {
            }
        }
    }

    private void checkWait(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            QuestObjective objective, int index) {

        long requiredMillis = DurationParser.parseMillis(objective.param("time", "0"));
        long elapsed = System.currentTimeMillis() - progress.stageStartedAtMillis();

        if (elapsed >= requiredMillis) {
            engine.completeObjectiveDirectly(player, quest, stage, progress, index);
        }
    }

    private void checkReachLocation(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            QuestObjective objective, int index) {

        String world = objective.param("world", null);
        if (world == null || !player.getWorld().getName().equals(world)) {
            return;
        }

        double x = parseDouble(objective.param("x", "0"));
        double y = parseDouble(objective.param("y", "0"));
        double z = parseDouble(objective.param("z", "0"));
        double radius = parseDouble(objective.param("radius", "3"));

        Location target = new Location(player.getWorld(), x, y, z);

        if (player.getLocation().distanceSquared(target) <= radius * radius) {
            engine.completeObjectiveDirectly(player, quest, stage, progress, index);
        }
    }

    private void checkDiscoverRegion(Player player, Quest quest, QuestStage stage, ActiveQuestProgress progress,
            QuestObjective objective, int index) {

        String regionId = objective.param("region", null);
        if (regionId == null) {
            return;
        }

        boolean inside = regionManager.findAt(player.getLocation())
                .map(region -> region.id().equalsIgnoreCase(regionId))
                .orElse(false);

        if (inside) {
            engine.completeObjectiveDirectly(player, quest, stage, progress, index);
        }
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
