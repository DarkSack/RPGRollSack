package com.sack.rpgroll.workers.core.worker;

import com.sack.rpgroll.workers.core.economy.WageType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerTest {

    private Worker worker() {
        return new Worker(UUID.randomUUID(), "farmer", PersonalityTrait.RESPONSIBLE);
    }

    @ParameterizedTest
    @CsvSource({"-10,0", "0,0", "50,50", "100,100", "150,100"})
    void needsAreClampedBetweenZeroAndHundred(double input, double expected) {
        Worker worker = worker();
        worker.setHunger(input);
        worker.setEnergy(input);
        worker.setSleep(input);
        worker.setStress(input);
        worker.setMotivation(input);
        worker.setHealth(input);
        worker.setHappiness(input);

        assertEquals(expected, worker.hunger());
        assertEquals(expected, worker.energy());
        assertEquals(expected, worker.sleep());
        assertEquals(expected, worker.stress());
        assertEquals(expected, worker.motivation());
        assertEquals(expected, worker.health());
        assertEquals(expected, worker.happiness());
    }

    @Test
    void moraleIsAverageOfSevenNeedsWithStressInverted() {
        Worker worker = worker();
        // Defaults: hunger 100, energy 100, sleep 100, stress 0, motivation 80, health 100, happiness 80.
        double expected = (100 + 100 + 100 + (100 - 0) + 80 + 100 + 80) / 7.0;

        assertEquals(expected, worker.morale(), 0.0001);
    }

    @Test
    void highStressLowersMoraleEvenWithOtherNeedsMaxed() {
        Worker worker = worker();
        worker.setStress(100);

        assertTrue(worker.morale() < 100);
    }

    @Test
    void addSkillExperienceLevelsUpWhenThresholdReached() {
        Worker worker = worker();
        boolean leveledUp = worker.addSkillExperience("farming", 100, 10, 100);

        assertTrue(leveledUp);
        assertEquals(1, worker.skillLevel("farming"));
        assertEquals(0, worker.skillExperience("farming"));
    }

    @Test
    void addSkillExperienceCanLevelUpMultipleTimesInOneCall() {
        Worker worker = worker();
        boolean leveledUp = worker.addSkillExperience("farming", 250, 10, 100);

        assertTrue(leveledUp);
        assertEquals(2, worker.skillLevel("farming"));
        assertEquals(50, worker.skillExperience("farming"));
    }

    @Test
    void addSkillExperienceStopsAtMaxLevelEvenWithExcessExperience() {
        Worker worker = worker();
        worker.addSkillExperience("farming", 1000, 2, 100);

        assertEquals(2, worker.skillLevel("farming"));
    }

    @Test
    void addSkillExperienceBelowThresholdDoesNotLevelUp() {
        Worker worker = worker();
        boolean leveledUp = worker.addSkillExperience("farming", 50, 10, 100);

        assertFalse(leveledUp);
        assertEquals(0, worker.skillLevel("farming"));
        assertEquals(50, worker.skillExperience("farming"));
    }

    @Test
    void isInventoryFullOnlyAtOrAboveCapacity() {
        Worker worker = worker();
        worker.addCarried("WHEAT", Worker.INVENTORY_CAPACITY - 1);
        assertFalse(worker.isInventoryFull());

        worker.addCarried("WHEAT", 1);
        assertTrue(worker.isInventoryFull());
    }

    @Test
    void carriedTotalSumsAcrossMultipleMaterials() {
        Worker worker = worker();
        worker.addCarried("WHEAT", 10);
        worker.addCarried("CARROT", 5);

        assertEquals(15, worker.carriedTotal());
    }

    @Test
    void clearCarriedEmptiesInventory() {
        Worker worker = worker();
        worker.addCarried("WHEAT", 10);
        worker.clearCarried();

        assertEquals(0, worker.carriedTotal());
        assertFalse(worker.isInventoryFull());
    }

    @Test
    void hireSetsEmployedStateAndFireClearsIt() {
        Worker worker = worker();
        UUID employer = UUID.randomUUID();
        worker.hire(employer, 10, WageType.HOURLY);

        assertTrue(worker.isEmployed());
        assertEquals(employer, worker.employerId());
        assertEquals(10, worker.wageAmount());
        assertEquals(WageType.HOURLY, worker.wageType());

        worker.fire();

        assertFalse(worker.isEmployed());
        assertEquals(0, worker.wageAmount());
    }

    @Test
    void eventWorkSpeedMultiplierIsOneWithoutActiveEvent() {
        Worker worker = worker();
        assertEquals(1.0, worker.eventWorkSpeedMultiplier());
    }

    @Test
    void triggerEventSetsActiveStateAndMultiplier() {
        Worker worker = worker();
        worker.triggerEvent("storm", 200, 0.5);

        assertTrue(worker.hasActiveEvent());
        assertEquals("storm", worker.activeWorkerEventId());
        assertEquals(200, worker.eventRemainingTicks());
        assertEquals(0.5, worker.eventWorkSpeedMultiplier());
    }

    @Test
    void reduceEventDurationClearsEventWhenItRunsOut() {
        Worker worker = worker();
        worker.triggerEvent("storm", 10, 0.5);
        worker.reduceEventDuration(10);

        assertFalse(worker.hasActiveEvent());
        assertEquals(1.0, worker.eventWorkSpeedMultiplier());
    }

    @Test
    void reduceEventDurationKeepsEventActiveWhenTimeRemains() {
        Worker worker = worker();
        worker.triggerEvent("storm", 10, 0.5);
        worker.reduceEventDuration(4);

        assertTrue(worker.hasActiveEvent());
        assertEquals(6, worker.eventRemainingTicks());
    }

    @Test
    void reduceEventDurationWithNoActiveEventIsNoOp() {
        Worker worker = worker();
        worker.reduceEventDuration(100);

        assertFalse(worker.hasActiveEvent());
    }

}
