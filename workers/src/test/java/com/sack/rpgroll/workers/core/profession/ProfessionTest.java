package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.ai.AiCondition;
import com.sack.rpgroll.workers.core.economy.WageType;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfessionTest {

    @Test
    void orderedRulesSortsAscendingByPriorityRegardlessOfInputOrder() {
        AiRule low = new AiRule(AiCondition.ALWAYS, AiAction.IDLE, 10);
        AiRule high = new AiRule(AiCondition.HUNGRY, AiAction.SEEK_FOOD, 1);
        AiRule mid = new AiRule(AiCondition.TIRED, AiAction.SLEEP, 5);

        Profession profession = new Profession("farmer", null, null, null, null, Set.of(), List.of(low, high, mid),
                null, 0, WageType.PER_TASK, null, null);

        List<AiRule> ordered = profession.orderedRules();

        assertEquals(high, ordered.get(0));
        assertEquals(mid, ordered.get(1));
        assertEquals(low, ordered.get(2));
    }

    @Test
    void entityTypeDefaultsToVillagerAndIsUppercased() {
        Profession lowercase = new Profession("farmer", null, null, null, "villager", Set.of(), List.of(), null, 0,
                WageType.PER_TASK, null, null);
        Profession blank = new Profession("farmer", null, null, null, "", Set.of(), List.of(), null, 0,
                WageType.PER_TASK, null, null);

        assertEquals("VILLAGER", lowercase.entityType());
        assertEquals("VILLAGER", blank.entityType());
    }

    @Test
    void toolMaterialIsUppercasedOrNullWhenBlank() {
        Profession withTool = new Profession("miner", null, null, null, null, Set.of(), List.of(), null, 0,
                WageType.PER_TASK, "iron_pickaxe", null);
        Profession withoutTool = new Profession("miner", null, null, null, null, Set.of(), List.of(), null, 0,
                WageType.PER_TASK, "  ", null);

        assertEquals("IRON_PICKAXE", withTool.toolMaterial());
        assertEquals(null, withoutTool.toolMaterial());
    }

    @Test
    void wageAmountCannotBeNegative() {
        Profession profession = new Profession("farmer", null, null, null, null, Set.of(), List.of(), null, -50,
                WageType.PER_TASK, null, null);

        assertEquals(0, profession.wageAmount());
    }

    @Test
    void scheduleIdBlankBecomesNull() {
        Profession profession = new Profession("farmer", null, null, null, null, Set.of(), List.of(), "  ", 0,
                WageType.PER_TASK, null, null);

        assertEquals(null, profession.scheduleId());
    }

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Profession(null, null, null, null, null, Set.of(), List.of(), null, 0, WageType.PER_TASK,
                        null, null));
    }

}
