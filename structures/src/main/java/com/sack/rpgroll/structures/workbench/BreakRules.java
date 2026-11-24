package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.config.StructuresSettings.BreakPolicy;

/** Qué pasa al romper la mesa. Lógica pura. */
public final class BreakRules {
    private BreakRules() {}

    public enum BreakDecision { DENY, ALLOW, ALLOW_PAUSE, ALLOW_CANCEL }

    public static BreakDecision decide(BreakPolicy policy, boolean hasActiveProject, boolean canManage) {
        if (!canManage) {
            return BreakDecision.DENY;
        }
        if (!hasActiveProject) {
            return BreakDecision.ALLOW;
        }
        return switch (policy) {
            case BLOCK -> BreakDecision.DENY;
            case PAUSE -> BreakDecision.ALLOW_PAUSE;
            case CANCEL -> BreakDecision.ALLOW_CANCEL;
        };
    }
}
