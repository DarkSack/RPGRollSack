package com.sack.rpgroll.crates.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateTest {

    private CrateReward reward() {
        return new CrateReward("r1", "Reward", Material.PAPER, List.of(), 1, false, List.of());
    }

    @Test
    void nullIdThrows() {
        assertThrows(NullPointerException.class,
                () -> new Crate(null, "n", "gui", true, null, null, null, null, List.of(reward())));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Crate("  ", "n", "gui", true, null, null, null, null, List.of(reward())));
    }

    @Test
    void nullRewardsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Crate("id", "n", "gui", true, null, null, null, null, null));
    }

    @Test
    void emptyRewardsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Crate("id", "n", "gui", true, null, null, null, null, List.of()));
    }

    @Test
    void blankGuiTitleFallsBackToDisplayName() {
        Crate crate = new Crate("id", "Legendary Crate", "  ", true, null, null, null, null, List.of(reward()));

        assertEquals("Legendary Crate", crate.guiTitle());
    }

    @Test
    void nullKeyMaterialFallsBackToTripwireHook() {
        Crate crate = new Crate("id", "n", "gui", true, null, null, null, null, List.of(reward()));

        assertEquals(Material.TRIPWIRE_HOOK, crate.keyMaterial());
    }

    @Test
    void blankKeyDisplayNameFallsBackToGeneratedLabel() {
        Crate crate = new Crate("id", "Legendary", "gui", true, null, "  ", null, null, List.of(reward()));

        assertEquals("Llave: Legendary", crate.keyDisplayName());
    }

    @Test
    void nullListsBecomeEmptyImmutableDefaults() {
        Crate crate = new Crate("id", "n", "gui", true, null, null, null, null, List.of(reward()));

        assertTrue(crate.keyLore().isEmpty());
        assertTrue(crate.hologramLines().isEmpty());
    }
}
