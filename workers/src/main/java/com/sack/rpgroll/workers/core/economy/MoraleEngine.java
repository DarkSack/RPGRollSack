package com.sack.rpgroll.workers.core.economy;

import com.sack.rpgroll.workers.core.worker.Worker;

/**
 * Traduce la moral general de un worker ({@code Worker#morale()}) a
 * efectos concretos: más rápidos y con menos errores cuando están
 * felices, más lentos y con más chance de renunciar cuando no.
 */
public class MoraleEngine {

    private static final double QUIT_MORALE_THRESHOLD = 15;
    private static final double QUIT_CHANCE_PER_CHECK = 0.05;

    private final java.util.Random random = new java.util.Random();

    public double workSpeedMultiplier(Worker worker) {

        double morale = worker.morale();

        if (morale >= 80) {
            return 1.2;
        }

        if (morale <= 30) {
            return 0.6;
        }

        return 1.0;
    }

    /** @return true si, con esta moral, el worker decide renunciar a su contrato actual. */
    public boolean shouldQuit(Worker worker) {

        if (!worker.isEmployed() || worker.morale() > QUIT_MORALE_THRESHOLD) {
            return false;
        }

        return random.nextDouble() < QUIT_CHANCE_PER_CHECK;
    }

}
