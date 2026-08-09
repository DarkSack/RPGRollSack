package com.sack.rpgroll.workers.core.ai;

import com.sack.rpgroll.workers.core.profession.AiRule;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.schedule.Schedule;
import com.sack.rpgroll.workers.core.schedule.ScheduleActivity;
import com.sack.rpgroll.workers.core.worker.Worker;

/**
 * Decide qué {@link AiAction} le toca a un worker — evalúa las
 * {@link AiRule} de su profesión en orden de prioridad, y la primera
 * cuya condición matchea gana. Si ninguna regla matchea (ej. el admin no
 * puso una regla {@code ALWAYS} de respaldo), cae al horario si tiene
 * uno (según la hora actual del mundo), o a {@code WORK} por defecto.
 */
public class AiEngine {

    private static final double HUNGRY_THRESHOLD = 30;
    private static final double TIRED_THRESHOLD = 30;
    private static final double SLEEPY_THRESHOLD = 20;
    private static final double STRESSED_THRESHOLD = 70;
    private static final double LOW_HAPPINESS_THRESHOLD = 30;

    public AiAction decide(Worker worker, Profession profession, Schedule schedule, long worldTick,
            WorldContext context) {

        for (AiRule rule : profession.orderedRules()) {
            if (matches(rule.condition(), worker, context)) {
                return rule.action();
            }
        }

        if (schedule != null) {
            return fromSchedule(schedule.activityAt(worldTick));
        }

        return AiAction.WORK;
    }

    private boolean matches(AiCondition condition, Worker worker, WorldContext context) {
        return switch (condition) {
            case HUNGRY -> worker.hunger() < HUNGRY_THRESHOLD;
            case TIRED -> worker.energy() < TIRED_THRESHOLD;
            case SLEEPY -> worker.sleep() < SLEEPY_THRESHOLD;
            case STRESSED -> worker.stress() > STRESSED_THRESHOLD;
            case INVENTORY_FULL -> worker.isInventoryFull();
            case RAINING -> context.raining();
            case NIGHT -> context.night();
            case LOW_HAPPINESS -> worker.happiness() < LOW_HAPPINESS_THRESHOLD;
            case HAS_TASK -> true;
            case ALWAYS -> true;
        };
    }

    private AiAction fromSchedule(ScheduleActivity activity) {
        return switch (activity) {
            case WAKE, REST, FREE -> AiAction.IDLE;
            case EAT -> AiAction.SEEK_FOOD;
            case WORK -> AiAction.WORK;
            case SLEEP -> AiAction.SLEEP;
        };
    }

}
