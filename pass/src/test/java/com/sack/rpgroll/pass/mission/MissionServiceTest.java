package com.sack.rpgroll.pass.mission;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionServiceTest {

    private static final List<Mission> POOL = IntStream.range(0, 10)
            .mapToObj(i -> new Mission("m" + i, "Misión " + i, MissionScope.DAILY, MissionType.KILL_MOB, "", 1, 100))
            .toList();

    @Test
    void theSameDayPicksTheSameMissionsForEveryone() {
        assertEquals(MissionService.pick(POOL, 3, 20_000), MissionService.pick(POOL, 3, 20_000));
    }

    @Test
    void picksDistinctMissionsAndChangesWithTheDay() {

        List<String> today = MissionService.pick(POOL, 3, 20_000);

        assertEquals(3, new HashSet<>(today).size());
        assertNotEquals(today, MissionService.pick(POOL, 3, 20_001));
    }

    @Test
    void aSmallPoolGivesWhatItHas() {
        assertEquals(2, MissionService.pick(POOL.subList(0, 2), 3, 1).size());
    }

    @Test
    void anEmptyTargetMatchesAnythingOfItsType() {

        Mission any = new Mission("a", "a", MissionScope.DAILY, MissionType.KILL_MOB, "", 1, 1);
        Mission zombies = new Mission("z", "z", MissionScope.DAILY, MissionType.KILL_MOB, "ZOMBIE", 1, 1);

        assertTrue(any.matches(MissionType.KILL_MOB, "SKELETON"));
        assertTrue(zombies.matches(MissionType.KILL_MOB, "zombie"));
        assertTrue(!zombies.matches(MissionType.KILL_MOB, "SKELETON"));
        assertTrue(!any.matches(MissionType.FISH, ""));
    }

}
