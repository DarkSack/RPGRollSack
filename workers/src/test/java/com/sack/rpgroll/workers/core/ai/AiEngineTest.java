package com.sack.rpgroll.workers.core.ai;

import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.AiRule;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.schedule.Schedule;
import com.sack.rpgroll.workers.core.schedule.ScheduleActivity;
import com.sack.rpgroll.workers.core.schedule.ScheduleEntry;
import com.sack.rpgroll.workers.core.worker.PersonalityTrait;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiEngineTest {

    private final AiEngine engine = new AiEngine();
    private static final WorldContext CALM = new WorldContext(false, false);

    private Worker freshWorker() {
        return new Worker(UUID.randomUUID(), "farmer", PersonalityTrait.RESPONSIBLE);
    }

    private Profession professionWithRules(AiRule... rules) {
        return new Profession("farmer", "Farmer", null, null, null, Set.of(), List.of(rules), null, 0,
                WageType.PER_TASK, null, null);
    }

    @Test
    void firstMatchingRuleByPriorityWins() {
        Worker worker = freshWorker();
        worker.setHunger(10);
        worker.setEnergy(10);

        Profession profession = professionWithRules(
                new AiRule(AiCondition.TIRED, AiAction.SLEEP, 5),
                new AiRule(AiCondition.HUNGRY, AiAction.SEEK_FOOD, 1));

        AiAction decision = engine.decide(worker, profession, null, 0, CALM);

        assertEquals(AiAction.SEEK_FOOD, decision);
    }

    @Test
    void ruleOrderInListDoesNotMatterOnlyPriorityDoes() {
        Worker worker = freshWorker();
        worker.setHunger(10);
        worker.setEnergy(10);

        Profession profession = professionWithRules(
                new AiRule(AiCondition.HUNGRY, AiAction.SEEK_FOOD, 5),
                new AiRule(AiCondition.TIRED, AiAction.SLEEP, 1));

        AiAction decision = engine.decide(worker, profession, null, 0, CALM);

        assertEquals(AiAction.SLEEP, decision);
    }

    @Test
    void fallsBackToScheduleWhenNoRuleMatches() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules();
        Schedule schedule = new Schedule("day", null, null, List.of(new ScheduleEntry(0, ScheduleActivity.SLEEP)));

        AiAction decision = engine.decide(worker, profession, schedule, 100, CALM);

        assertEquals(AiAction.SLEEP, decision);
    }

    @Test
    void fallsBackToWorkWhenNoRuleMatchesAndNoSchedule() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules();

        AiAction decision = engine.decide(worker, profession, null, 0, CALM);

        assertEquals(AiAction.WORK, decision);
    }

    @Test
    void inventoryFullConditionMatchesWhenWorkerCarriesCapacity() {
        Worker worker = freshWorker();
        worker.addCarried("WHEAT", Worker.INVENTORY_CAPACITY);

        Profession profession = professionWithRules(new AiRule(AiCondition.INVENTORY_FULL, AiAction.GO_TO_WAREHOUSE, 1));

        assertEquals(AiAction.GO_TO_WAREHOUSE, engine.decide(worker, profession, null, 0, CALM));
    }

    @Test
    void rainingConditionReadsFromWorldContextNotWorker() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules(new AiRule(AiCondition.RAINING, AiAction.SEEK_SHELTER, 1));

        assertEquals(AiAction.SEEK_SHELTER, engine.decide(worker, profession, null, 0, new WorldContext(true, false)));
        assertEquals(AiAction.WORK, engine.decide(worker, profession, null, 0, CALM));
    }

    @Test
    void nightConditionReadsFromWorldContext() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules(new AiRule(AiCondition.NIGHT, AiAction.GO_HOME, 1));

        assertEquals(AiAction.GO_HOME, engine.decide(worker, profession, null, 0, new WorldContext(false, true)));
    }

    @Test
    void stressedConditionMatchesAboveThresholdNotAtThreshold() {
        Worker worker = freshWorker();
        worker.setStress(70);

        Profession profession = professionWithRules(new AiRule(AiCondition.STRESSED, AiAction.IDLE, 1));

        assertEquals(AiAction.WORK, engine.decide(worker, profession, null, 0, CALM));

        worker.setStress(71);
        assertEquals(AiAction.IDLE, engine.decide(worker, profession, null, 0, CALM));
    }

    @Test
    void alwaysConditionMatchesRegardlessOfWorkerState() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules(new AiRule(AiCondition.ALWAYS, AiAction.IDLE, 10));

        assertEquals(AiAction.IDLE, engine.decide(worker, profession, null, 0, CALM));
    }

    @Test
    void scheduleWakeRestAndFreeAllMapToIdle() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules();

        for (ScheduleActivity activity : List.of(ScheduleActivity.WAKE, ScheduleActivity.REST, ScheduleActivity.FREE)) {
            Schedule schedule = new Schedule("day", null, null, List.of(new ScheduleEntry(0, activity)));
            assertEquals(AiAction.IDLE, engine.decide(worker, profession, schedule, 0, CALM));
        }
    }

    @Test
    void scheduleEatMapsToSeekFood() {
        Worker worker = freshWorker();
        Profession profession = professionWithRules();
        Schedule schedule = new Schedule("day", null, null, List.of(new ScheduleEntry(0, ScheduleActivity.EAT)));

        assertEquals(AiAction.SEEK_FOOD, engine.decide(worker, profession, schedule, 0, CALM));
    }

}
