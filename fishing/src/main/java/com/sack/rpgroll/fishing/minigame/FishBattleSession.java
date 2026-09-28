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
 *
 * <p>Cada paso del forcejeo es una llamada a {@link #tick()} (el manager la hace cada
 * {@code FishingMinigameManager.TICK_INTERVAL} ticks del servidor).
 */
public class FishBattleSession {

    /** Pasos antes de que el pez se escape solo: 300 pasos de 2 ticks = 30 s. */
    static final int MAX_DURATION_STEPS = 300;

    /** Resultado de un golpe. */
    public enum Swing {
        HIT,
        MISS,
        /** Otro golpe en la misma pasada por la zona: no cuenta ni penaliza. */
        IGNORED
    }

    private final CatchResult pendingCatch;
    private final double oscillationSpeed;
    private final double zoneHalfWidth;
    private final boolean erratic;

    private int requiredHits;
    private int allowedMisses;
    private int elapsedSteps;
    private double zoneCenter;
    /** Ya se acertó en esta pasada del indicador por la zona: hasta que salga, no cuenta otro acierto. */
    private boolean zoneSpent;
    private final Random random;

    public FishBattleSession(CatchResult pendingCatch, double rodResistance, double rodReelSpeed) {
        this(pendingCatch, rodResistance, rodReelSpeed, new Random());
    }

    FishBattleSession(CatchResult pendingCatch, double rodResistance, double rodReelSpeed, Random random) {

        this.pendingCatch = pendingCatch;
        this.random = random;

        FishBehaviorType behavior = pendingCatch.species().behavior();
        double weightRatio = weightRatio(pendingCatch);

        this.oscillationSpeed = switch (behavior) {
            case FAST, AGGRESSIVE -> 0.35;
            case JUMPER -> 0.25;
            case SHY, ELUSIVE -> 0.28;
            case SLOW -> 0.15;
        };

        double baseHalfWidth = switch (behavior) {
            case SHY, ELUSIVE -> 0.09;
            case FAST -> 0.12;
            case SLOW -> 0.22;
            default -> 0.15;
        };
        // reel-speed > 1 agranda la zona (más fácil), < 1 la achica; acotado para que siga siendo un juego.
        this.zoneHalfWidth = Math.min(0.4, baseHalfWidth * Math.max(0.5, Math.min(2.0, rodReelSpeed)));

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

    /** Posición actual del indicador (0.0-1.0). */
    public double meterPosition() {
        return 0.5 + 0.5 * Math.sin(elapsedSteps * oscillationSpeed);
    }

    public boolean isInZone(double meterPosition) {
        return Math.abs(meterPosition - zoneCenter) <= zoneHalfWidth;
    }

    /** Un golpe del jugador con el indicador donde está ahora. */
    public Swing swing() {

        if (!isInZone(meterPosition())) {
            allowedMisses--;
            return Swing.MISS;
        }

        if (zoneSpent) {
            return Swing.IGNORED;
        }

        zoneSpent = true;
        requiredHits--;
        return Swing.HIT;
    }

    public void tick() {

        elapsedSteps++;

        if (erratic && elapsedSteps % 40 == 0) {
            zoneCenter = 0.2 + random.nextDouble() * 0.6;
        }

        if (!isInZone(meterPosition())) {
            zoneSpent = false;
        }
    }

    public double zoneCenter() {
        return zoneCenter;
    }

    public double zoneHalfWidth() {
        return zoneHalfWidth;
    }

    public boolean isWon() {
        return requiredHits <= 0;
    }

    public boolean isLost() {
        return allowedMisses < 0 || elapsedSteps >= MAX_DURATION_STEPS;
    }

    public int elapsedSteps() {
        return elapsedSteps;
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
