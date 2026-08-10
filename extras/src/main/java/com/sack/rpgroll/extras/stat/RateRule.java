package com.sack.rpgroll.extras.stat;

/** Una regla de regeneración condicional (sección 4) — {@code condition} null/vacío = siempre aplica. */
public record RateRule(String condition, double amount) {

    public static RateRule unconditional(double amount) {
        return new RateRule(null, amount);
    }

}
