package com.sack.rpgroll.quests.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Define una misión completa: identidad, restricciones de repetición,
 * requisitos para iniciarla, el flujo de stages que la componen, sus
 * recompensas finales y sus eventos de ciclo de vida (on-start,
 * on-complete, on-fail, on-abandon — on-progress/on-stage-change viven a
 * nivel de stage, ver {@link QuestStage}).
 */
public record Quest(
        String id,
        String displayName,
        QuestCategory category,
        QuestDifficulty difficulty,
        boolean repeatable,
        long cooldownMillis,
        QuestRequirements requirements,
        List<QuestStage> stages,
        QuestRewards rewards,
        Map<QuestEventType, List<QuestAction>> events) implements RPGContent {

    public Quest {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (stages == null || stages.isEmpty()) {
            throw new IllegalArgumentException("la quest '" + id + "' necesita al menos un stage");
        }

        category = category == null ? QuestCategory.SIDE_QUEST : category;
        difficulty = difficulty == null ? QuestDifficulty.NORMAL : difficulty;
        requirements = requirements == null ? QuestRequirements.none() : requirements;
        stages = List.copyOf(stages);
        rewards = rewards == null ? QuestRewards.none() : rewards;
        events = events == null ? Map.of() : Map.copyOf(events);
    }

    public List<QuestAction> events(QuestEventType type) {
        return events.getOrDefault(type, List.of());
    }

    public QuestStage firstStage() {
        return stages.get(0);
    }

    public Optional<QuestStage> stage(String stageId) {
        return stages.stream().filter(stage -> stage.id().equals(stageId)).findFirst();
    }

    public Optional<QuestStage> stageAt(int index) {
        return index >= 0 && index < stages.size() ? Optional.of(stages.get(index)) : Optional.empty();
    }

    public int indexOfStage(String stageId) {
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).id().equals(stageId)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isLastStage(int index) {
        return index == stages.size() - 1;
    }

}
