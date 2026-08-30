package com.sack.rpgroll.dungeons.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonDefinitionTest {

    private DungeonDefinition minimal(List<DungeonRoom> rooms) {
        return new DungeonDefinition("crypt", null, null, null, null, 1, 15, 1, 5, 0, true, null, null, null,
                rooms, null, null, null, null, null);
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new DungeonDefinition("", null, null, null, null, 1, 15, 1, 5, 0, true, null, null, null,
                        null, null, null, null, null, null));
    }

    @Test
    void maxPlayersIsClampedToAtLeastMinPlayers() {
        DungeonDefinition def = new DungeonDefinition("crypt", null, null, null, null, 1, 15, 5, 2, 0, true,
                null, null, null, null, null, null, null, null, null);

        assertEquals(5, def.minPlayers());
        assertEquals(5, def.maxPlayers());
    }

    @Test
    void emptyDifficultiesFallsBackToDefaultNormal() {
        DungeonDefinition def = minimal(List.of());

        assertEquals(1, def.difficulties().size());
        assertEquals("normal", def.difficulties().get(0).id());
    }

    @Test
    void roomLookupIsCaseInsensitive() {
        DungeonRoom room = new DungeonRoom("Entrance", DungeonRoomType.ENTRANCE, null, null, null, null, null);
        DungeonDefinition def = minimal(List.of(room));

        assertTrue(def.room("entrance").isPresent());
        assertTrue(def.room("ENTRANCE").isPresent());
        assertFalse(def.room("missing").isPresent());
    }

    @Test
    void indexOfRoomReturnsMinusOneWhenNotFound() {
        DungeonRoom room = new DungeonRoom("room1", DungeonRoomType.COMBAT, null, null, null, null, null);
        DungeonDefinition def = minimal(List.of(room));

        assertEquals(0, def.indexOfRoom("room1"));
        assertEquals(-1, def.indexOfRoom("nope"));
    }

    @Test
    void isLastRoomTrueOnlyAtOrPastFinalIndex() {
        DungeonRoom room1 = new DungeonRoom("room1", DungeonRoomType.COMBAT, null, null, null, null, null);
        DungeonRoom room2 = new DungeonRoom("room2", DungeonRoomType.BOSS, null, null, null, null, null);
        DungeonDefinition def = minimal(List.of(room1, room2));

        assertFalse(def.isLastRoom(0));
        assertTrue(def.isLastRoom(1));
        assertTrue(def.isLastRoom(5));
    }

    @Test
    void roomAtOutOfBoundsReturnsEmpty() {
        DungeonDefinition def = minimal(List.of());

        assertTrue(def.roomAt(-1).isEmpty());
        assertTrue(def.roomAt(0).isEmpty());
    }

    @Test
    void actionsForUnmappedTriggerReturnsEmptyList() {
        DungeonDefinition def = minimal(List.of());
        assertTrue(def.actionsFor(DungeonTrigger.values()[0]).isEmpty());
    }

    @Test
    void difficultyLookupIsCaseInsensitive() {
        DungeonDifficulty hard = new DungeonDifficulty("Hard", "Hard", 2.0, 2.0, 1.0, List.of());
        DungeonDefinition def = new DungeonDefinition("crypt", null, null, null, null, 1, 15, 1, 5, 0, true,
                null, null, null, null, List.of(hard), null, null, null, null);

        assertTrue(def.difficulty("hard").isPresent());
        assertTrue(def.difficulty("HARD").isPresent());
    }
}
