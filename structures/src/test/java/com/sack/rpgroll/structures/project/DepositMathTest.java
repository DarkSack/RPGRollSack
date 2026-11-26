package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.storage.MaterialRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DepositMathTest {

    static MaterialSpec spec(String key, int amount, boolean optional) {
        return new MaterialSpec(List.of(key), amount, optional);
    }

    @Test void toTakeNeverExceedsMissingOrAvailable() {
        var specs = List.of(spec("a", 10, false), spec("b", 5, false), spec("c", 5, false));
        var rows = Map.of("a", new MaterialRow(10, 7, 0), "b", new MaterialRow(5, 0, 0), "c", new MaterialRow(5, 5, 0));
        var take = DepositMath.toTake(specs, rows, Map.of("a", 100, "b", 2), null);
        assertEquals(Map.of("a", 3, "b", 2), take);
        assertEquals(Map.of("b", 2), DepositMath.toTake(specs, rows, Map.of("a", 100, "b", 2), "b"));
    }

    @Test void optionalDoesNotBlockComplete() {
        var specs = List.of(spec("a", 10, false), spec("o", 4, true));
        var rows = Map.of("a", new MaterialRow(10, 10, 0), "o", new MaterialRow(4, 0, 0));
        assertTrue(DepositMath.complete(specs, rows));
        assertFalse(DepositMath.complete(specs, Map.of("a", new MaterialRow(10, 9, 0), "o", new MaterialRow(4, 4, 0))));
    }

    @Test void consumedIsProportionalAndCapped() {
        assertEquals(Map.of("a", 33), DepositMath.consumedAt(Map.of("a", new MaterialRow(100, 100, 0)), 1, 3));
        assertEquals(Map.of("a", 20), DepositMath.consumedAt(Map.of("a", new MaterialRow(100, 20, 0)), 3, 3));
    }

    @Test void refundIsUnspentTimesPercent() {
        var rows = Map.of("a", new MaterialRow(100, 100, 30), "b", new MaterialRow(5, 5, 5));
        assertEquals(Map.of("a", 35, "b", 0), DepositMath.refund(rows, 50));
        assertEquals(Map.of("a", 70, "b", 0), DepositMath.refund(rows, 100));
    }

    @Test void duplicateKeyIsTakenOnce() {
        var specs = List.of(spec("a", 6, false), spec("a", 4, false));
        var rows = Map.of("a", new MaterialRow(10, 7, 0));
        assertEquals(Map.of("a", 3), DepositMath.toTake(specs, rows, Map.of("a", 100), null));
    }

    @Test void refundClampsPercent() {
        var rows = Map.of("a", new MaterialRow(10, 10, 0));
        assertEquals(Map.of("a", 10), DepositMath.refund(rows, 250));
        assertEquals(Map.of("a", 0), DepositMath.refund(rows, -5));
    }

    @Test void specWithoutRowCountsAsMet() {
        // Recarga que añadió un material: no formaba parte del precio de esta obra.
        var specs = List.of(spec("a", 10, false), spec("nuevo", 5, false));
        assertTrue(DepositMath.complete(specs, Map.of("a", new MaterialRow(10, 10, 0))));
        assertFalse(DepositMath.complete(specs, Map.of("a", new MaterialRow(10, 9, 0))));
        assertEquals(List.of("nuevo"), DepositMath.missingRows(specs, Map.of("a", new MaterialRow(10, 10, 0))));
    }
}
