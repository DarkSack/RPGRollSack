package com.sack.rpgroll.structures.workbench;

import org.junit.jupiter.api.Test;

import static com.sack.rpgroll.structures.config.StructuresSettings.BreakPolicy.*;
import static com.sack.rpgroll.structures.workbench.BreakRules.BreakDecision.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BreakRulesTest {
    @Test void breakPolicyTable() {
        assertEquals(DENY, BreakRules.decide(BLOCK, true, true));
        assertEquals(ALLOW_PAUSE, BreakRules.decide(PAUSE, true, true));
        assertEquals(ALLOW_CANCEL, BreakRules.decide(CANCEL, true, true));
        assertEquals(ALLOW, BreakRules.decide(BLOCK, false, true));
        assertEquals(DENY, BreakRules.decide(PAUSE, false, false));
        assertEquals(DENY, BreakRules.decide(CANCEL, true, false));
    }
}
