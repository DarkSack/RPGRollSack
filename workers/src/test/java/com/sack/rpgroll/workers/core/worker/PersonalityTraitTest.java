package com.sack.rpgroll.workers.core.worker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonalityTraitTest {

    @Test
    void lazyIsSlowerAndMoreStressResistant() {
        assertEquals(0.7, PersonalityTrait.LAZY.workSpeedMultiplier());
        assertEquals(0.7, PersonalityTrait.LAZY.stressResistance());
        assertEquals(1.0, PersonalityTrait.LAZY.errorChanceMultiplier());
    }

    @Test
    void fastIsFasterWithNoOtherModifiers() {
        assertEquals(1.3, PersonalityTrait.FAST.workSpeedMultiplier());
        assertEquals(1.0, PersonalityTrait.FAST.stressResistance());
        assertEquals(1.0, PersonalityTrait.FAST.errorChanceMultiplier());
    }

    @Test
    void clumsyDoublesErrorChanceButHasDefaultSpeed() {
        assertEquals(1.0, PersonalityTrait.CLUMSY.workSpeedMultiplier());
        assertEquals(2.0, PersonalityTrait.CLUMSY.errorChanceMultiplier());
    }

    @Test
    void smartAndResponsibleHalveErrorChance() {
        assertEquals(0.5, PersonalityTrait.SMART.errorChanceMultiplier());
        assertEquals(0.5, PersonalityTrait.RESPONSIBLE.errorChanceMultiplier());
    }

    @Test
    void ambitiousAndCowardAreLessStressResistant() {
        assertEquals(1.3, PersonalityTrait.AMBITIOUS.stressResistance());
        assertEquals(1.3, PersonalityTrait.COWARD.stressResistance());
    }

    @Test
    void everyTraitHasNonZeroPositiveMultipliers() {
        for (PersonalityTrait trait : PersonalityTrait.values()) {
            assertEquals(true, trait.workSpeedMultiplier() > 0);
            assertEquals(true, trait.stressResistance() > 0);
            assertEquals(true, trait.errorChanceMultiplier() > 0);
        }
    }

}
