package com.sack.rpgroll.structures.protect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Qué conjuntos de regiones se evalúan, con un WorldGuard falso: decide el nivel de prioridad más alto. */
class WorldGuardGuardTest {

    /** Región falsa: prioridad, si el dueño es miembro y si contiene la caja entera. */
    record R(String id, int priority, boolean member, boolean containsBox) {
    }

    /** Como FlagValueCalculator con BUILD: sin regiones se puede; si no, todas las del nivel más alto con miembro. */
    static boolean wg(List<R> set) {
        if (set.isEmpty()) {
            return true;
        }
        int top = set.stream().mapToInt(R::priority).max().orElseThrow();
        return set.stream().filter(r -> r.priority() == top).allMatch(R::member);
    }

    static boolean allowed(R... crossing) {
        return WorldGuardGuard.allowed(List.of(crossing), R::containsBox, WorldGuardGuardTest::wg);
    }

    @Test void boxStickingOutOfTheOwnersParcelIntoTheCityIsDenied() {
        R parcel = new R("parcela", 10, true, false), city = new R("ciudad", 0, false, true);
        assertTrue(wg(List.of(parcel, city)), "el conjunto entero lo dejaría pasar (la parcela tapa la ciudad)");
        assertFalse(allowed(parcel, city));
    }

    @Test void boxFullyInsideTheParcelIsAllowed() {
        assertTrue(allowed(new R("parcela", 10, true, true), new R("ciudad", 0, false, true)));
    }

    @Test void wildernessPlusTheOwnersOwnRegionIsAllowed() {
        assertTrue(allowed(new R("mia", 0, true, false)));
        assertTrue(allowed(), "solo naturaleza");
    }

    @Test void touchingSomeoneElsesRegionIsDenied() {
        assertFalse(allowed(new R("mia", 5, true, false), new R("ajena", 0, false, false)));
    }
}
