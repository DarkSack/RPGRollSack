package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.crafting.station.tier.TierUpgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomStationTest {

    private CustomStation station(int inventorySize, int maxTier, List<TierUpgrade> tierUpgrades) {
        return new CustomStation("forge", null, null, null, inventorySize, null, -1, 0, false, null, null, null,
                maxTier, tierUpgrades, 0, 0, null, false);
    }

    @ParameterizedTest
    @CsvSource({
            "9, 9",
            "27, 27",
            "10, 9",
            "35, 27",
            "0, 9",
            "-5, 9",
            "60, 54",
            "54, 54",
    })
    void normalizeSizeRoundsDownToNearestMultipleOfNineWithinRange(int requested, int expected) {
        assertEquals(expected, station(requested, 1, null).inventorySize());
    }

    @Test
    void defaultsApplyWhenOptionalFieldsAreNullOrBlank() {
        CustomStation station = station(27, 1, null);

        assertEquals("forge", station.displayName());
        assertEquals("SMITHING_TABLE", station.icon());
        assertEquals("SMITHING_TABLE", station.triggerBlockMaterial());
        assertEquals("forge", station.guiTitle());
        assertEquals("forge", station.skillCategory());
        assertTrue(station.ingredientSlots().isEmpty());
        assertTrue(station.allowedRecipeIds().isEmpty());
    }

    @Test
    void maxTierClampsToAtLeastOne() {
        assertEquals(1, station(27, 0, null).maxTier());
        assertEquals(1, station(27, -3, null).maxTier());
    }

    @Test
    void nextTierUpgradeEmptyWhenAlreadyAtMaxTier() {
        CustomStation station = station(27, 2, List.of(new TierUpgrade(2, null, 0, null)));

        assertTrue(station.nextTierUpgrade(2).isEmpty());
    }

    @Test
    void nextTierUpgradeFindsMatchingUpgradeForNextTier() {
        TierUpgrade upgradeToTier2 = new TierUpgrade(2, null, 100, null);
        CustomStation station = station(27, 3, List.of(upgradeToTier2, new TierUpgrade(3, null, 200, null)));

        Optional<TierUpgrade> next = station.nextTierUpgrade(1);

        assertTrue(next.isPresent());
        assertEquals(2, next.get().tier());
        assertEquals(100, next.get().economyCost());
    }

    @Test
    void nextTierUpgradeEmptyWhenNoConfiguredUpgradeMatchesNextTier() {
        CustomStation station = station(27, 3, List.of(new TierUpgrade(3, null, 200, null)));

        assertFalse(station.nextTierUpgrade(1).isPresent());
    }
}
