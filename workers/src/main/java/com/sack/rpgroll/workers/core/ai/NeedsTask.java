package com.sack.rpgroll.workers.core.ai;

import com.sack.rpgroll.workers.core.worker.PersonalityTrait;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Corre cada {@code needs-decay-interval-ticks}: decae hambre/energía/
 * sueño y sube estrés (salvo mientras el worker está {@code SLEEP}ando,
 * que restaura en vez de decaer), y arrastra felicidad/motivación hacia
 * un promedio de las demás necesidades — ignorar las necesidades básicas
 * termina bajando felicidad/motivación solas, sin un chequeo aparte.
 */
public class NeedsTask extends BukkitRunnable {

    private static final double MIN_HEALTH_FLOOR = 10;

    private final WorkerManager workerManager;
    private final double hungerDecay;
    private final double energyDecay;
    private final double sleepDecay;
    private final double stressGain;

    public NeedsTask(WorkerManager workerManager, double hungerDecay, double energyDecay, double sleepDecay,
            double stressGain) {
        this.workerManager = workerManager;
        this.hungerDecay = hungerDecay;
        this.energyDecay = energyDecay;
        this.sleepDecay = sleepDecay;
        this.stressGain = stressGain;
    }

    @Override
    public void run() {

        for (Worker worker : List.copyOf(workerManager.getAll())) {

            PersonalityTrait personality = worker.personality();

            if (worker.currentAction() == AiAction.SLEEP) {
                worker.setEnergy(worker.energy() + 5);
                worker.setSleep(worker.sleep() + 8);
                worker.setStress(worker.stress() - 3 * personality.stressResistance());
            } else {
                worker.setHunger(worker.hunger() - hungerDecay);
                worker.setEnergy(worker.energy() - energyDecay);
                worker.setSleep(worker.sleep() - sleepDecay);
                worker.setStress(worker.stress() + stressGain * personality.stressResistance());
            }

            double moraleTarget = (worker.hunger() + worker.energy() + worker.sleep() + (100 - worker.stress())) / 4.0;
            worker.setHappiness(worker.happiness() + (moraleTarget - worker.happiness()) * 0.1);
            worker.setMotivation(worker.motivation() + (moraleTarget - worker.motivation()) * 0.1);

            if (worker.hunger() < 10 || worker.sleep() < 10) {
                worker.setHealth(Math.max(MIN_HEALTH_FLOOR, worker.health() - 1));
            } else if (worker.health() < 100 && worker.happiness() > 60) {
                worker.setHealth(Math.min(100, worker.health() + 0.5));
            }
        }
    }

}
