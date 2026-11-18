package com.sack.rpgroll.quests.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryProgressTest {

    private static Quest quest(String id, QuestCategory category, int level) {
        QuestRequirements requirements = new QuestRequirements(level, null, null, null, null, null, 0,
                null, null, null, null, null, null, -1, -1);
        return new Quest(id, id, category, null, false, 0, requirements,
                List.of(new QuestStage("s1", null, null, null, null)), null, null);
    }

    private final List<Quest> quests = List.of(
            quest("camino_10", QuestCategory.MAIN_STORY, 36),
            quest("camino_2", QuestCategory.MAIN_STORY, 4),
            quest("encargo", QuestCategory.DAILY, 1),
            quest("camino_1", QuestCategory.MAIN_STORY, 1),
            quest("camino_3", QuestCategory.MAIN_STORY, 8));

    @Test
    void newPlayerGetsTheFirstChapter() {
        var step = StoryProgress.current(quests, QuestCategory.MAIN_STORY, id -> false, id -> false).orElseThrow();

        assertEquals("camino_1", step.quest().id());
        assertFalse(step.active());
    }

    @Test
    void anActiveChapterWinsOverTheNextOne() {
        var step = StoryProgress.current(quests, QuestCategory.MAIN_STORY,
                "camino_2"::equals, "camino_1"::equals).orElseThrow();

        assertEquals("camino_2", step.quest().id());
        assertTrue(step.active());
    }

    @Test
    void afterCompletingChaptersTheNextOneInOrderIsOffered() {
        Set<String> done = Set.of("camino_1", "camino_2", "camino_3");

        var step = StoryProgress.current(quests, QuestCategory.MAIN_STORY, id -> false, done::contains).orElseThrow();

        assertEquals("camino_10", step.quest().id());
    }

    @Test
    void finishedStoryHasNoStep() {
        Set<String> done = Set.of("camino_1", "camino_2", "camino_3", "camino_10");

        assertTrue(StoryProgress.current(quests, QuestCategory.MAIN_STORY, id -> false, done::contains).isEmpty());
    }

    @Test
    void sameLevelChaptersUseNaturalIdOrder() {
        assertTrue(StoryProgress.compareNatural("parte_2", "parte_10") < 0);
        assertTrue(StoryProgress.compareNatural("a", "b") < 0);
    }
}
