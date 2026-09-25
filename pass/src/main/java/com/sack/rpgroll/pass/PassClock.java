package com.sack.rpgroll.pass;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;

/**
 * El "hoy" del pase: los diarios y las misiones cambian a medianoche de la
 * zona horaria configurada, no a la del servidor ni en UTC.
 */
public final class PassClock {

    private final Clock clock;

    public PassClock(ZoneId zone) {
        this(Clock.system(zone));
    }

    public PassClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public long epochDay() {
        return today().toEpochDay();
    }

    /** Semana ISO, p. ej. "2026-W39": las misiones semanales cambian el lunes. */
    public String weekKey() {
        LocalDate today = today();
        return today.get(IsoFields.WEEK_BASED_YEAR) + "-W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

}
