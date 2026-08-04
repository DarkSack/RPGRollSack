package com.sack.rpgroll.seasons.runtime;

/**
 * Progreso de UN reloj de calendario — identificado por una clave libre
 * ({@code "world:<nombre>"} o {@code "region:<id>"}, ver {@link
 * SeasonClockManager}). Mutable, vive en memoria y se persiste vía {@link
 * SeasonStateStore}.
 */
public class CalendarState {

    private final String clockKey;
    private String calendarId;
    private int seasonIndex;
    private int subSeasonIndex = -1;
    private long elapsedTicks;
    private int year = 1;
    private long lastWorldEventDayRoll = -1;

    public CalendarState(String clockKey, String calendarId) {
        this.clockKey = clockKey;
        this.calendarId = calendarId;
    }

    public String clockKey() {
        return clockKey;
    }

    public String calendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
        this.seasonIndex = 0;
        this.subSeasonIndex = -1;
        this.elapsedTicks = 0;
    }

    public int seasonIndex() {
        return seasonIndex;
    }

    public int subSeasonIndex() {
        return subSeasonIndex;
    }

    public long elapsedTicks() {
        return elapsedTicks;
    }

    public void addElapsedTicks(long ticks) {
        this.elapsedTicks += ticks;
    }

    public int year() {
        return year;
    }

    public long lastWorldEventDayRoll() {
        return lastWorldEventDayRoll;
    }

    public void setLastWorldEventDayRoll(long day) {
        this.lastWorldEventDayRoll = day;
    }

    /** Avanza a la siguiente subestación (o estación si no hay más) — resetea el contador de ticks. */
    public void advanceSubSeason() {
        subSeasonIndex++;
        elapsedTicks = 0;
    }

    /** Avanza a la siguiente estación del calendario, sumando un año si vuelve al principio. */
    public void advanceSeason(int seasonCount) {

        seasonIndex++;
        subSeasonIndex = -1;
        elapsedTicks = 0;

        if (seasonIndex >= seasonCount) {
            seasonIndex = 0;
            year++;
        }
    }

    /** Fuerza una estación específica (comando admin) — no toca el año. */
    public void setSeasonIndex(int index) {
        this.seasonIndex = index;
        this.subSeasonIndex = -1;
        this.elapsedTicks = 0;
    }

    /** Restaura un estado ya conocido tal cual (carga desde disco) — no dispara ningún avance/reset. */
    public void restoreExact(int seasonIndex, int subSeasonIndex, long elapsedTicks, int year,
            long lastWorldEventDayRoll) {
        this.seasonIndex = seasonIndex;
        this.subSeasonIndex = subSeasonIndex;
        this.elapsedTicks = elapsedTicks;
        this.year = year;
        this.lastWorldEventDayRoll = lastWorldEventDayRoll;
    }

}
