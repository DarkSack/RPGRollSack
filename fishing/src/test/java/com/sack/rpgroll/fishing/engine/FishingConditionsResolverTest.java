package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.DepthRequirement;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishingConditionsResolverTest {

    @Test
    void aPuddleHasSurfaceAndBottomButNoMidWater() {
        assertEquals(Set.of(DepthRequirement.SURFACE, DepthRequirement.BOTTOM),
                FishingConditionsResolver.depthsFor(2, true, false));
    }

    @Test
    void deepWaterOffersEveryLayerSoBottomFishCanBeCaught() {
        assertEquals(Set.of(DepthRequirement.SURFACE, DepthRequirement.MID_WATER, DepthRequirement.BOTTOM),
                FishingConditionsResolver.depthsFor(25, true, false));
    }

    @Test
    void withoutAFloorInReachThereIsNoBottom() {
        assertEquals(Set.of(DepthRequirement.SURFACE, DepthRequirement.MID_WATER),
                FishingConditionsResolver.depthsFor(40, false, false));
    }

    @Test
    void aRoofAddsTheCaveLayer() {
        assertEquals(Set.of(DepthRequirement.SURFACE, DepthRequirement.MID_WATER, DepthRequirement.BOTTOM,
                DepthRequirement.UNDERWATER_CAVE), FishingConditionsResolver.depthsFor(5, true, true));
    }

}
