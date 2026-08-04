package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.engine.DungeonEngine;

/** Corre cada segundo: timers de sala/oleada, objetivos por tiempo, revivir por temporizador y modificadores. */
public class DungeonSessionTask implements Runnable {

    private final DungeonEngine engine;

    public DungeonSessionTask(DungeonEngine engine) {
        this.engine = engine;
    }

    @Override
    public void run() {
        engine.tick();
    }

}
