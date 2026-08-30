package com.sack.rpgroll.quests.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestTest {

    private QuestStage stage(String id) {
        return new QuestStage(id, null, null, null, null);
    }

    @Test
    void constructorRejectsNullIdOrDisplayName() {
        assertThrows(NullPointerException.class,
                () -> new Quest(null, "Name", null, null, false, 0, null, List.of(stage("s1")), null, null));
        assertThrows(NullPointerException.class,
                () -> new Quest("quest-1", null, null, null, false, 0, null, List.of(stage("s1")), null, null));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Quest("  ", "Name", null, null, false, 0, null, List.of(stage("s1")), null, null));
    }

    @Test
    void constructorRejectsNullOrEmptyStages() {
        assertThrows(IllegalArgumentException.class,
                () -> new Quest("quest-1", "Name", null, null, false, 0, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Quest("quest-1", "Name", null, null, false, 0, null, List.of(), null, null));
    }

    @Test
    void constructorAppliesDefaultsForOptionalFields() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null, List.of(stage("s1")), null, null);

        assertEquals(QuestCategory.SIDE_QUEST, quest.category());
        assertEquals(QuestDifficulty.NORMAL, quest.difficulty());
        assertEquals(QuestRequirements.none(), quest.requirements());
        assertEquals(QuestRewards.none(), quest.rewards());
        assertTrue(quest.events().isEmpty());
    }

    @Test
    void firstStageReturnsStageAtIndexZero() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null,
                List.of(stage("intro"), stage("finale")), null, null);

        assertEquals("intro", quest.firstStage().id());
    }

    @Test
    void stageLooksUpByIdReturningEmptyWhenNotFound() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null,
                List.of(stage("intro"), stage("finale")), null, null);

        assertEquals("finale", quest.stage("finale").map(QuestStage::id).orElse(null));
        assertTrue(quest.stage("nonexistent").isEmpty());
    }

    @Test
    void stageAtReturnsEmptyForOutOfRangeIndex() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null, List.of(stage("intro")), null, null);

        assertTrue(quest.stageAt(-1).isEmpty());
        assertTrue(quest.stageAt(1).isEmpty());
        assertEquals(Optional.of(stage("intro").id()), quest.stageAt(0).map(QuestStage::id));
    }

    @Test
    void indexOfStageReturnsMinusOneWhenNotFound() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null,
                List.of(stage("intro"), stage("finale")), null, null);

        assertEquals(0, quest.indexOfStage("intro"));
        assertEquals(1, quest.indexOfStage("finale"));
        assertEquals(-1, quest.indexOfStage("nonexistent"));
    }

    @Test
    void isLastStageChecksAgainstFinalIndex() {
        Quest quest = new Quest("quest-1", "Name", null, null, false, 0, null,
                List.of(stage("intro"), stage("finale")), null, null);

        assertFalse(quest.isLastStage(0));
        assertTrue(quest.isLastStage(1));
    }
}
