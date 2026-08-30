package com.sack.rpgroll.quests.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestStageTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class, () -> new QuestStage(null, null, null, null, null));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> new QuestStage("  ", null, null, null, null));
    }

    @Test
    void constructorDefaultsNullCollectionsToEmpty() {
        QuestStage stage = new QuestStage("stage-1", null, null, null, null);

        assertTrue(stage.objectives().isEmpty());
        assertTrue(stage.conditions().isEmpty());
        assertTrue(stage.events().isEmpty());
    }

    @Test
    void eventsReturnsEmptyListForUnconfiguredType() {
        QuestStage stage = new QuestStage("stage-1", null, null, null, null);

        assertTrue(stage.events(QuestEventType.ON_START).isEmpty());
    }

    @Test
    void eventsReturnsConfiguredActionsForMatchingType() {
        QuestAction action = new QuestAction("MESSAGE", Map.of("text", "hi"));
        QuestStage stage = new QuestStage("stage-1", null, null, null,
                Map.of(QuestEventType.ON_START, List.of(action)));

        assertEquals(List.of(action), stage.events(QuestEventType.ON_START));
        assertTrue(stage.events(QuestEventType.ON_COMPLETE).isEmpty());
    }
}
