package com.sack.rpgroll.crates.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateRewardTest {

    @Test
    void nullIdThrows() {
        assertThrows(NullPointerException.class,
                () -> new CrateReward(null, "n", Material.PAPER, List.of(), 1, false, List.of()));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new CrateReward("  ", "n", Material.PAPER, List.of(), 1, false, List.of()));
    }

    @Test
    void nullDisplayNameThrows() {
        assertThrows(NullPointerException.class,
                () -> new CrateReward("id", null, Material.PAPER, List.of(), 1, false, List.of()));
    }

    @Test
    void nullIconThrows() {
        assertThrows(NullPointerException.class,
                () -> new CrateReward("id", "n", null, List.of(), 1, false, List.of()));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -0.0001})
    void nonPositiveWeightThrows(double weight) {
        assertThrows(IllegalArgumentException.class,
                () -> new CrateReward("id", "n", Material.PAPER, List.of(), weight, false, List.of()));
    }

    @Test
    void nullLoreAndActionsBecomeEmptyImmutableLists() {
        CrateReward reward = new CrateReward("id", "n", Material.PAPER, null, 1, false, null);

        assertTrue(reward.lore().isEmpty());
        assertTrue(reward.actions().isEmpty());
    }
}
