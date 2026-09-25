package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.ascension.progress.TriggerType;

import java.util.List;

/**
 * Las cuentas de un logro, sin nada de Bukkit: cuánto lleva cada criterio y
 * si ya está completo. {@link AchievementEngine} pone el resto (quién es el
 * jugador, qué nivel tiene, qué se entrega).
 */
public final class AchievementProgress {

    /** De dónde sale el valor actual de un criterio de estado (nivel, prestigio…). */
    @FunctionalInterface
    public interface StateValues {
        int valueOf(Criterion criterion);
    }

    private AchievementProgress() {
    }

    /** Clave del progreso de un criterio en el estado del jugador. */
    public static String key(Achievement achievement, int index) {
        return achievement.id() + "#" + index;
    }

    /**
     * Suma el evento a los criterios de contador que encajan.
     *
     * @return true si cambió algo
     */
    public static boolean apply(AscensionPlayerState state, Achievement achievement, ProgressEvent event) {

        boolean changed = false;
        List<Criterion> criteria = achievement.criteria();

        for (int i = 0; i < criteria.size(); i++) {

            Criterion criterion = criteria.get(i);

            if (!criterion.type().isCounter() || !criterion.matches(event)) {
                continue;
            }

            String key = key(achievement, i);
            int current = state.getAchievementProgress(key);

            if (current >= required(criterion)) {
                continue;
            }

            if (criterion.type() == TriggerType.VISIT_BIOME) {
                // Cada bioma cuenta una vez: volver al mismo no suma.
                if (!state.addAchievementDistinct(key, event.target())) {
                    continue;
                }
                state.setAchievementProgress(key, state.getAchievementDistinct().get(key).size());
            } else {
                state.setAchievementProgress(key, current + 1);
            }

            changed = true;
        }

        return changed;
    }

    public static int current(AscensionPlayerState state, Achievement achievement, int index, StateValues values) {

        Criterion criterion = achievement.criteria().get(index);

        return criterion.type().isCounter()
                ? state.getAchievementProgress(key(achievement, index))
                : values.valueOf(criterion);
    }

    public static int required(Criterion criterion) {
        return criterion.type().isCounter() ? Math.max(1, criterion.amount()) : criterion.amount();
    }

    /** Un logro sin criterios nunca se completa solo: es de los que se otorgan a mano. */
    public static boolean isComplete(AscensionPlayerState state, Achievement achievement, StateValues values) {

        if (achievement.criteria().isEmpty()) {
            return false;
        }

        for (int i = 0; i < achievement.criteria().size(); i++) {
            if (current(state, achievement, i, values) < required(achievement.criteria().get(i))) {
                return false;
            }
        }

        return true;
    }

}
