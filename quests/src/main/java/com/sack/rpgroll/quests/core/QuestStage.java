package com.sack.rpgroll.quests.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una etapa de una quest: uno o más objetivos simultáneos, condiciones que
 * deben cumplirse para poder avanzar, un diálogo opcional, y eventos
 * propios (on-start al entrar al stage, on-progress en cada avance de
 * objetivo, on-complete al terminar todos sus objetivos).
 */
public record QuestStage(
        String id,
        List<QuestObjective> objectives,
        List<String> conditions,
        Dialog dialog,
        Map<QuestEventType, List<QuestAction>> events) {

    public QuestStage {
        Objects.requireNonNull(id, "id de stage no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id de stage no puede estar vacío");
        }

        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        events = events == null ? Map.of() : Map.copyOf(events);
    }

    public List<QuestAction> events(QuestEventType type) {
        return events.getOrDefault(type, List.of());
    }

}
