package com.sack.rpgroll.tab.animation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationDefinitionTest {

    @Test
    void emptyFramesReturnsEmptyString() {
        AnimationDefinition def = new AnimationDefinition("id", AnimationType.FRAME, 5, List.of());

        assertEquals("", def.frameAt(0));
        assertEquals("", def.frameAt(1234));
    }

    @Test
    void cyclesThroughFramesAsTickCounterAdvances() {
        AnimationDefinition def = new AnimationDefinition("id", AnimationType.FRAME, 1,
                List.of("a", "b", "c"));

        assertEquals("a", def.frameAt(0));
        assertEquals("b", def.frameAt(1));
        assertEquals("c", def.frameAt(2));
        assertEquals("a", def.frameAt(3));
        assertEquals("c", def.frameAt(5));
    }

    @Test
    void advancesOneFrameOnlyEveryIntervalTicks() {
        AnimationDefinition def = new AnimationDefinition("id", AnimationType.FRAME, 5,
                List.of("a", "b"));

        assertEquals("a", def.frameAt(0));
        assertEquals("a", def.frameAt(4));
        assertEquals("b", def.frameAt(5));
        assertEquals("b", def.frameAt(9));
        assertEquals("a", def.frameAt(10));
    }

    @Test
    void zeroOrNegativeIntervalIsTreatedAsOneTickPerFrame() {
        AnimationDefinition zeroInterval = new AnimationDefinition("id", AnimationType.FRAME, 0,
                List.of("a", "b"));
        AnimationDefinition negativeInterval = new AnimationDefinition("id", AnimationType.FRAME, -5,
                List.of("a", "b"));

        assertEquals("b", zeroInterval.frameAt(1));
        assertEquals("b", negativeInterval.frameAt(1));
    }

    @Test
    void singleFrameAlwaysReturnsThatFrame() {
        AnimationDefinition def = new AnimationDefinition("id", AnimationType.FRAME, 1, List.of("only"));

        assertEquals("only", def.frameAt(0));
        assertEquals("only", def.frameAt(9999));
    }
}
