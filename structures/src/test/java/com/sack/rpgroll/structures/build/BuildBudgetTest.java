package com.sack.rpgroll.structures.build;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildBudgetTest {

    private final UUID a = new UUID(0, 1), b = new UUID(0, 2), c = new UUID(0, 3);

    @Test
    void budgetNeverExceedsPerTick() {
        BuildBudget budget = new BuildBudget(10);
        Map<UUID, Integer> wanted = Map.of(a, 100, b, 3, c, 100);
        for (int i = 0; i < 5; i++) {
            Map<UUID, Integer> got = budget.share(List.of(a, b, c), wanted);
            assertTrue(got.values().stream().mapToInt(Integer::intValue).sum() <= 10);
            got.forEach((k, v) -> assertTrue(v <= wanted.get(k)));
        }
    }

    @Test
    void budgetRotatesBetweenProjects() {
        BuildBudget budget = new BuildBudget(10);
        Map<UUID, Integer> wanted = Map.of(a, 100, b, 100, c, 100);
        Map<UUID, Integer> total = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            budget.share(List.of(a, b, c), wanted).forEach((k, v) -> total.merge(k, v, Integer::sum));
        }
        for (UUID id : List.of(a, b, c)) {
            assertTrue(total.getOrDefault(id, 0) >= 10, id + " recibio " + total.get(id));
        }
    }

    @Test
    void smallRequestsAreFullyServed() {
        Map<UUID, Integer> got = new BuildBudget(10).share(List.of(a, b), Map.of(a, 2, b, 3));
        assertEquals(2, got.get(a));
        assertEquals(3, got.get(b));
    }
}
