package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;
import com.sack.rpgroll.ascension.reward.Rewards;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementProgressTest {

    private static final AchievementProgress.StateValues NO_STATE = criterion -> 0;

    private AscensionPlayerState state;

    @BeforeEach
    void setUp() {
        state = new AscensionPlayerState(UUID.randomUUID());
    }

    private static Achievement achievement(Criterion... criteria) {
        return new Achievement("test", "Test", "", false, List.of(criteria), Rewards.none());
    }

    @Test
    void counterCompletesAtItsAmountAndStopsCounting() {
        Achievement kills = achievement(new Criterion(TriggerType.KILL_ENTITY, "ZOMBIE", null, null, 3));
        ProgressEvent zombie = new ProgressEvent(TriggerType.KILL_ENTITY, "ZOMBIE", null);

        AchievementProgress.apply(state, kills, zombie);
        AchievementProgress.apply(state, kills, zombie);
        assertFalse(AchievementProgress.isComplete(state, kills, NO_STATE));

        assertTrue(AchievementProgress.apply(state, kills, zombie));
        assertTrue(AchievementProgress.isComplete(state, kills, NO_STATE));

        assertFalse(AchievementProgress.apply(state, kills, zombie), "ya completo, no sigue sumando");
        assertEquals(3, state.getAchievementProgress(AchievementProgress.key(kills, 0)));
    }

    @Test
    void eventsThatDoNotMatchAreIgnored() {
        Achievement kills = achievement(new Criterion(TriggerType.KILL_ENTITY, "ZOMBIE", null, null, 1));

        assertFalse(AchievementProgress.apply(state, kills, new ProgressEvent(TriggerType.KILL_ENTITY, "SKELETON", null)));
        assertFalse(AchievementProgress.isComplete(state, kills, NO_STATE));
    }

    @Test
    void biomesCountOnceEach() {
        Achievement explorer = achievement(new Criterion(TriggerType.VISIT_BIOME, null, null, null, 2));

        AchievementProgress.apply(state, explorer, new ProgressEvent(TriggerType.VISIT_BIOME, "minecraft:plains", null));
        assertFalse(AchievementProgress.apply(state, explorer,
                new ProgressEvent(TriggerType.VISIT_BIOME, "minecraft:plains", null)), "volver al mismo no cuenta");
        assertFalse(AchievementProgress.isComplete(state, explorer, NO_STATE));

        AchievementProgress.apply(state, explorer, new ProgressEvent(TriggerType.VISIT_BIOME, "terralith:yosemite_cliffs", null));
        assertTrue(AchievementProgress.isComplete(state, explorer, NO_STATE));
    }

    @Test
    void allCriteriaMustBeMetIncludingStateOnes() {
        Achievement legend = achievement(
                new Criterion(TriggerType.KILL_ENTITY, "ENDER_DRAGON", null, null, 1),
                new Criterion(TriggerType.REACH_LEVEL, null, null, null, 50));

        AchievementProgress.apply(state, legend, new ProgressEvent(TriggerType.KILL_ENTITY, "ENDER_DRAGON", null));

        assertFalse(AchievementProgress.isComplete(state, legend, criterion -> 49));
        assertTrue(AchievementProgress.isComplete(state, legend, criterion -> 50));
    }

    @Test
    void achievementWithoutCriteriaNeverCompletesOnItsOwn() {
        Achievement manual = new Achievement("manual", "Manual", "");
        assertFalse(AchievementProgress.isComplete(state, manual, criterion -> 1000));
    }

    @Test
    void clearingProgressForgetsCountersAndBiomes() {
        Achievement explorer = achievement(new Criterion(TriggerType.VISIT_BIOME, null, null, null, 5));
        AchievementProgress.apply(state, explorer, new ProgressEvent(TriggerType.VISIT_BIOME, "minecraft:plains", null));

        state.clearAchievementProgress("test");

        assertEquals(0, state.getAchievementProgress(AchievementProgress.key(explorer, 0)));
        assertTrue(state.getAchievementDistinct().isEmpty());
    }

}
