package com.sack.rpgroll.gameplay.levelup;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * La XP acumulada que pide cada nivel, a partir de los niveles que fija levelup-rewards.yml.
 * <p>
 * El archivo solo define algunos niveles (2, 3, 5, 10, 15, 20…). Antes, los que faltaban usaban la fórmula
 * {@code 100 · (nivel − 1)^1.5}, mucho más plana: el 21 pedía 9623 cuando el 20 pedía 35000, y al llegar al 20
 * se saltaban de golpe varios niveles. Ahora un nivel que falta se interpola entre los dos definidos que lo
 * rodean (de forma geométrica, como crece la curva), y por encima del último sigue creciendo con la fórmula.
 * El nivel 1 pide 0.
 */
final class ExpCurve {

    private static final int BASE_EXP = 100;
    private static final double MULTIPLIER = 1.5;

    private final NavigableMap<Integer, Integer> anchors;

    /** {@code anchors}: nivel → XP de los niveles que trae el archivo con {@code exp_required}. */
    ExpCurve(NavigableMap<Integer, Integer> anchors) {
        this.anchors = new TreeMap<>(anchors);
        this.anchors.put(1, 0);
    }

    int expRequired(int level) {
        if (level <= 1) {
            return 0;
        }
        Integer exact = anchors.get(level);
        if (exact != null) {
            return exact;
        }
        Map.Entry<Integer, Integer> low = anchors.lowerEntry(level);
        Map.Entry<Integer, Integer> high = anchors.higherEntry(level);
        if (high == null) {
            return low.getValue() + formula(level) - formula(low.getKey());
        }
        double t = (level - low.getKey()) / (double) (high.getKey() - low.getKey());
        int lo = low.getValue();
        int hi = high.getValue();
        int value = lo <= 0
                ? (int) Math.round(lo + (hi - lo) * t)
                : (int) Math.round(lo * Math.pow(hi / (double) lo, t));
        // Entre dos niveles con poca diferencia el redondeo podría repetir un valor: cada nivel pide al menos 1 más.
        int steps = level - low.getKey();
        return Math.min(hi - (high.getKey() - level), Math.max(lo + steps, value));
    }

    /** Avance de {@code from} (XP de su nivel) a {@code to} (XP del siguiente), de 0 a 100. */
    static int percent(int experience, int from, int to) {
        if (to <= from) {
            return 100;
        }
        long progress = Math.max(0, Math.min(experience - (long) from, to - (long) from));
        return (int) (progress * 100 / (to - from));
    }

    static int formula(int level) {
        return (int) (BASE_EXP * Math.pow(level - 1, MULTIPLIER));
    }
}
