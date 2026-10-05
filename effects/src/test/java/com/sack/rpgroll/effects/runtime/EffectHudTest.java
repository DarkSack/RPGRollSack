package com.sack.rpgroll.effects.runtime;

import net.kyori.adventure.bossbar.BossBar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectHudTest {

    @Test
    void mapsChatColorsToTheClosestBossBarColor() {
        assertEquals(BossBar.Color.YELLOW, EffectHud.colorOf("GOLD"));
        assertEquals(BossBar.Color.BLUE, EffectHud.colorOf("aqua"));
        assertEquals(BossBar.Color.RED, EffectHud.colorOf("DARK_RED"));
        assertEquals(BossBar.Color.PURPLE, EffectHud.colorOf("DARK_PURPLE"));
    }

    @Test
    void unknownOrMissingColorFallsBackToWhite() {
        assertEquals(BossBar.Color.WHITE, EffectHud.colorOf("NOT_A_COLOR"));
        assertEquals(BossBar.Color.WHITE, EffectHud.colorOf(null));
    }

}
