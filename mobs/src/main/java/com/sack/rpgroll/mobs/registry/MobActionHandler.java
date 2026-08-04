package com.sack.rpgroll.mobs.registry;

import com.sack.rpgroll.mobs.core.MobAction;

@FunctionalInterface
public interface MobActionHandler {
    void execute(MobAction action, MobActionContext context);
}
