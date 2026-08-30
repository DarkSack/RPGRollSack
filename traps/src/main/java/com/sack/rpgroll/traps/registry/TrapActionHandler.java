package com.sack.rpgroll.traps.registry;

import com.sack.rpgroll.traps.core.TrapAction;

@FunctionalInterface
public interface TrapActionHandler {
    void execute(TrapAction action, TrapActionContext context);
}
