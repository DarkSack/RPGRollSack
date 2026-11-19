package com.sack.rpgroll.fishing.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sack.rpgroll.fishing.core.CatchQuality;
import org.junit.jupiter.api.Test;

class MachineCatchSourceTest {

    @Test
    void clampLimitsQualityToTheCap() {
        assertEquals(CatchQuality.EXCELLENT, MachineCatchSource.clamp(CatchQuality.MASTERWORK, 2));
        assertEquals(CatchQuality.GOOD, MachineCatchSource.clamp(CatchQuality.GOOD, 2));
        assertEquals(CatchQuality.COMMON, MachineCatchSource.clamp(CatchQuality.GOOD, -1));
    }

    @Test
    void priceFollowsTheNewQuality() {
        assertEquals(30.0 / 3.0 * 1.7, MachineCatchSource.price(30.0, CatchQuality.MASTERWORK, CatchQuality.EXCELLENT), 1e-9);
    }
}
