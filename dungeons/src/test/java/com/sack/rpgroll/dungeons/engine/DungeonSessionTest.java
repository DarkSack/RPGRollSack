package com.sack.rpgroll.dungeons.engine;

import com.sack.rpgroll.dungeons.core.DungeonDifficulty;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonSessionTest {

    private final UUID playerA = UUID.randomUUID();
    private final UUID playerB = UUID.randomUUID();

    private DungeonSession twoPlayerSession() {
        return new DungeonSession("crypt", DungeonDifficulty.defaultNormal(), List.of(playerA, playerB));
    }

    @Test
    void startsInRunningStateAtRoomZero() {
        DungeonSession session = twoPlayerSession();

        assertEquals(DungeonSessionState.RUNNING, session.state());
        assertEquals(0, session.currentRoomIndex());
        assertEquals(0, session.checkpointRoomIndex());
        assertEquals(2, session.partySize());
    }

    @Test
    void enterRoomResetsWaveAndObjectiveState() {
        DungeonSession session = twoPlayerSession();
        session.startWave(0, 5);
        session.addObjectiveProgress(0, 3);
        session.trackSpawnedEntity(UUID.randomUUID());

        session.enterRoom(1);

        assertEquals(1, session.currentRoomIndex());
        assertEquals(-1, session.activeWaveIndex());
        assertEquals(0, session.waveMobsRemaining());
        assertEquals(0, session.getObjectiveProgress(0));
        assertTrue(session.currentRoomEntityIds().isEmpty());
    }

    @Test
    void enterRoomDoesNotClearGlobalSpawnedEntityHistory() {
        DungeonSession session = twoPlayerSession();
        UUID entity = UUID.randomUUID();
        session.trackSpawnedEntity(entity);

        session.enterRoom(1);

        assertTrue(session.spawnedEntityIds().contains(entity));
        assertFalse(session.currentRoomEntityIds().contains(entity));
    }

    @Test
    void decrementWaveMobsRemainingNeverGoesNegative() {
        DungeonSession session = twoPlayerSession();
        session.startWave(0, 1);

        session.decrementWaveMobsRemaining();
        session.decrementWaveMobsRemaining();

        assertEquals(0, session.waveMobsRemaining());
    }

    @Test
    void untrackEntityRemovesFromBothSets() {
        DungeonSession session = twoPlayerSession();
        UUID entity = UUID.randomUUID();
        session.trackSpawnedEntity(entity);

        session.untrackEntity(entity);

        assertFalse(session.spawnedEntityIds().contains(entity));
        assertFalse(session.currentRoomEntityIds().contains(entity));
    }

    @Test
    void addObjectiveProgressAccumulates() {
        DungeonSession session = twoPlayerSession();
        session.addObjectiveProgress(0, 2);
        session.addObjectiveProgress(0, 3);

        assertEquals(5, session.getObjectiveProgress(0));
        assertEquals(0, session.getObjectiveProgress(1));
    }

    @Test
    void totalDamageSumsAllPlayerContributions() {
        DungeonSession session = twoPlayerSession();
        session.addDamageContribution(playerA, 10.0);
        session.addDamageContribution(playerB, 5.0);
        session.addDamageContribution(playerA, 2.0);

        assertEquals(17.0, session.totalDamage());
        assertEquals(12.0, session.damageContribution().get(playerA));
    }

    @Test
    void isFullyWipedOnlyWhenAllPartyMembersAreDead() {
        DungeonSession session = twoPlayerSession();

        assertFalse(session.isFullyWiped());

        session.markDead(playerA);
        assertFalse(session.isFullyWiped());

        session.markDead(playerB);
        assertTrue(session.isFullyWiped());
    }

    @Test
    void markRevivedRemovesPlayerFromDeadSetButKeepsDeathCount() {
        DungeonSession session = twoPlayerSession();
        session.markDead(playerA);
        session.markRevived(playerA);

        assertFalse(session.deadPlayers().contains(playerA));
        assertEquals(1, session.deathCount());
        assertEquals(0, session.deathTimestamp(playerA));
    }

    @Test
    void decrementSharedLivesNeverGoesBelowZero() {
        DungeonSession session = twoPlayerSession();
        session.setSharedLivesRemaining(1);

        session.decrementSharedLives();
        session.decrementSharedLives();

        assertEquals(0, session.sharedLivesRemaining());
    }

    @Test
    void decrementSharedLivesLeavesNegativeUnlimitedValueUntouched() {
        DungeonSession session = twoPlayerSession();
        session.setSharedLivesRemaining(-1);

        session.decrementSharedLives();

        assertEquals(-1, session.sharedLivesRemaining());
    }
}
