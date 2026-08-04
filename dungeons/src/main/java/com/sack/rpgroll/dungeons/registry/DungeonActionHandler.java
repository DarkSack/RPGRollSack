package com.sack.rpgroll.dungeons.registry;

import com.sack.rpgroll.dungeons.core.DungeonAction;

@FunctionalInterface
public interface DungeonActionHandler {
    void execute(DungeonAction action, DungeonActionContext context);
}
