package com.sack.rpgroll.fishing.minigame;

import com.sack.rpgroll.fishing.core.FishBehaviorType;
import com.sack.rpgroll.fishing.engine.CatchResult;

import java.util.Random;

/**
 * Estado de UN forcejeo en curso — la posición del indicador oscila con
 * una onda seno; el jugador tiene que "reelear" (swing de brazo, ver
 * {@code FishingMinigameManager}) mientras el indicador esté dentro de la
 * zona objetivo. JUMPER re-centra la zona cada tanto para simular un pez
 * errático; el resto de los comportamientos solo ajustan ancho/velocidad.
 */
public class FishBattleSession {

    private static final int MAX_DURATION_TICKS = 300;

    private final CatchResult pendingCatch;
    private final double oscillationSpeed;
    private final double zoneHalfWidth;
    private final boolean erratic;

    private int requiredHits;
    private int allowedMisses;
    private int elapsedTicks;
    private double zoneCenter;
    private final Random random = new Random();

    public FishBattleSession(CatchResult pendingCatch, double rodResistance) {

        this.pendingCatch = pendingCatch;

        FishBehaviorType behavior = pendingCatch.species().behavior();
        double weightRatio = weightRatio(pendingCatch);

        this.oscillationSpeed = switch (behavior) {
            case FAST, AGGRESSIVE -> 0.35;
            case JUMPER -> 0.25;
            case SHY, ELUSIVE -> 0.28;
            case SLOW -> 0.15;
        };

        this.zoneHalfWidth = switch (behavior) {
            case SHY, ELUSIVE -> 0.09;
            case FAST -> 0.12;
            case SLOW -> 0.22;
            default -> 0.15;
        };

        this.erratic = behavior == FishBehaviorType.JUMPER;
        this.zoneCenter = 0.5;

        int baseHits = 3 + (int) Math.round(weightRatio * 3);
        this.requiredHits = Math.max(2, baseHits);

        int baseMisses = behavior == FishBehaviorType.ELUSIVE ? 2 : 4;
        this.allowedMisses = Math.max(1, (int) Math.round(baseMisses * Math.max(0.5, rodResistance)));
    }

    private double weightRatio(CatchResult catchResult) {

        var species = catchResult.species();
        double range = species.maxWeight() - species.minWeight();

        if (range <= 0) {
            return 0.5;
        }

        return Math.max(0, Math.min(1, (catchResult.weight() - species.minWeight()) / range));
    }

    /** Posición actual del indicador (0.0-1.0) — llamado cada tick por el manager. */
    public double meterPosition(int tick) {
        return 0.5 + 0.5 * Math.sin(tick * oscillationSpeed);
    }

    public boolean isInZone(double meterPosition) {
        return Math.abs(meterPosition - zoneCenter) <= zoneHalfWidth;
    }

    public void maybeReRandomizeZone(int tick) {

        if (erratic && tick % 40 == 0) {
            zoneCenter = 0.2 + random.nextDouble() * 0.6;
        }
    }

    public double zoneCenter() {
        return zoneCenter;
    }

    public double zoneHalfWidth() {
        return zoneHalfWidth;
    }

    public void registerHit() {
        requiredHits--;
    }

    public void registerMiss() {
        allowedMisses--;
    }

    public boolean isWon() {
        return requiredHits <= 0;
    }

    public boolean isLost() {
        return allowedMisses < 0 || elapsedTicks >= MAX_DURATION_TICKS;
    }

    public void tick() {
        elapsedTicks++;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public int requiredHits() {
        return requiredHits;
    }

    public int allowedMisses() {
        return allowedMisses;
    }

    public CatchResult pendingCatch() {
        return pendingCatch;
    }

}
