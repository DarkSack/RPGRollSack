package com.sack.rpgroll.tab.animation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressBarRendererTest {

    @Test
    void zeroPercentIsAllEmpty() {
        assertEquals("░░░░░░░░░░", ProgressBarRenderer.render(0, 10, '█', '░'));
    }

    @Test
    void hundredPercentIsAllFilled() {
        assertEquals("██████████", ProgressBarRenderer.render(100, 10, '█', '░'));
    }

    @Test
    void fiftyPercentIsHalfFilled() {
        assertEquals("█████░░░░░", ProgressBarRenderer.render(50, 10, '█', '░'));
    }

    @ParameterizedTest
    @CsvSource({
            "-50, 0",
            "-0.001, 0",
            "0, 0",
            "150, 10",
            "100.001, 10",
            "100, 10"
    })
    void percentOutsideZeroToHundredIsClamped(double percent, int expectedFilled) {
        String bar = ProgressBarRenderer.render(percent, 10, '█', '░');
        long filledCount = bar.chars().filter(c -> c == '█').count();

        assertEquals(expectedFilled, filledCount);
    }

    @Test
    void zeroLengthProducesEmptyString() {
        assertEquals("", ProgressBarRenderer.render(50, 0, '█', '░'));
    }

    @Test
    void roundsToNearestFilledSegmentRatherThanTruncating() {
        // 55% of length 10 = 5.5 filled -> rounds to 6
        assertEquals("██████░░░░", ProgressBarRenderer.render(55, 10, '█', '░'));
    }
}
