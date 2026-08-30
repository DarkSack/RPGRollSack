package com.sack.rpgroll.extras.expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericComparisonTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankOrNullExpressionAlwaysMatches(String expression) {
        assertTrue(NumericComparison.evaluate(expression, 12345));
    }

    @Test
    void noOperatorPrefixDefaultsToEquals() {
        assertTrue(NumericComparison.evaluate("30", 30));
        assertFalse(NumericComparison.evaluate("30", 30.1));
    }

    @ParameterizedTest
    @CsvSource({
            "<=30, 30, true",
            "<=30, 30.0001, false",
            ">=30, 29.9999, false",
            ">40, 40, false",
            ">40, 40.0001, true",
            "<40, 39.9999, true",
            "==5, 5, true",
            "!=5, 5, false",
            "!=5, 5.5, true"
    })
    void evaluatesEachOperatorAtItsBoundary(String expression, double value, boolean expected) {
        assertEquals(expected, NumericComparison.evaluate(expression, value));
    }

    @Test
    void malformedExpressionDoesNotMatch() {
        assertFalse(NumericComparison.evaluate("~=30", 30));
        assertFalse(NumericComparison.evaluate("abc", 30));
        assertFalse(NumericComparison.evaluate("<=", 30));
    }

    @Test
    void toleratesWhitespaceAroundExpression() {
        assertTrue(NumericComparison.evaluate("  <= 30  ", 30));
    }

    @Test
    void supportsNegativeTargets() {
        assertTrue(NumericComparison.evaluate("<=-5", -10));
        assertFalse(NumericComparison.evaluate("<=-5", 0));
    }
}
