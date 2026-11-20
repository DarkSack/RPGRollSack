package com.sack.rpgroll.gameplay.levelup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

/** La XP de los niveles que no define levelup-rewards.yml sale de los que sí define. */
class ExpCurveTest {

    /** Las anclas del levelup-rewards.yml de fábrica. */
    private static final Map<Integer, Integer> SHIPPED = Map.of(2, 150, 3, 325, 5, 1000, 10, 5000,
            15, 15000, 20, 35000, 25, 65000, 30, 110000, 50, 700000, 100, 10000000);

    @Test
    void definedLevelsKeepTheirExp() {
        ExpCurve curve = new ExpCurve(new TreeMap<>(SHIPPED));
        assertEquals(35000, curve.expRequired(20));
        assertEquals(150, curve.expRequired(2));
        assertEquals(10000000, curve.expRequired(100));
    }

    @Test
    void theCurveNeverGoesDownFromLevelTwoToTheTop() {
        ExpCurve curve = new ExpCurve(new TreeMap<>(SHIPPED));
        for (int level = 3; level <= 100; level++) {
            assertTrue(curve.expRequired(level) > curve.expRequired(level - 1),
                    "nivel " + level + ": " + curve.expRequired(level) + " <= " + curve.expRequired(level - 1));
        }
    }

    @Test
    void aMissingLevelSitsBetweenItsNeighbours() {
        ExpCurve curve = new ExpCurve(new TreeMap<>(SHIPPED));
        int level21 = curve.expRequired(21);
        assertTrue(level21 > 35000 && level21 < 65000, "nivel 21: " + level21);
    }

    @Test
    void aboveTheLastDefinedLevelItKeepsGrowing() {
        ExpCurve curve = new ExpCurve(new TreeMap<>(Map.of(10, 5000)));
        assertTrue(curve.expRequired(11) > 5000);
        assertTrue(curve.expRequired(12) > curve.expRequired(11));
    }

    @Test
    void percentGoesFromTheCurrentLevelToTheNext() {
        assertEquals(0, ExpCurve.percent(35000, 35000, 39613));
        assertEquals(50, ExpCurve.percent(37000, 35000, 39000));
        assertEquals(100, ExpCurve.percent(50000, 35000, 39613), "con XP de sobra no pasa de 100");
        assertEquals(0, ExpCurve.percent(8944, 35000, 39613), "con menos XP que su nivel no baja de 0");
        assertEquals(100, ExpCurve.percent(10, 35000, 35000));
    }

    @Test
    void withoutAnchorsItUsesTheFormula() {
        ExpCurve curve = new ExpCurve(new TreeMap<>());
        assertEquals((int) (100 * Math.pow(19, 1.5)), curve.expRequired(20));
    }
}
