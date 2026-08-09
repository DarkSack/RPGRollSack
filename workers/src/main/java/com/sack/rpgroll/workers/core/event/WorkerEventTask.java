package com.sack.rpgroll.workers.core.event;

import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Corre cada {@code worker-event-check-interval-ticks}: hace avanzar (y
 * limpiar al terminar) el evento activo de cada worker, y sortea uno
 * nuevo — {@code ILLNESS}/{@code ACCIDENT}/{@code STRIKE} solo si la
 * moral está baja, {@code PROMOTION} solo si está alta, {@code
 * VACATION} en cualquier momento para un worker empleado.
 */
public class WorkerEventTask extends BukkitRunnable {

    private static final double HIGH_MORALE_THRESHOLD = 80;

    private final WorkerManager workerManager;
    private final WorkerEventManager eventManager;
    private final long intervalTicks;
    private final double baseChance;
    private final double lowMoraleThreshold;
    private final Random random = new Random();

    public WorkerEventTask(WorkerManager workerManager, WorkerEventManager eventManager, long intervalTicks,
            double baseChance, double lowMoraleThreshold) {
        this.workerManager = workerManager;
        this.eventManager = eventManager;
        this.intervalTicks = intervalTicks;
        this.baseChance = baseChance;
        this.lowMoraleThreshold = lowMoraleThreshold;
    }

    @Override
    public void run() {

        for (Worker worker : List.copyOf(workerManager.getAll())) {

            if (worker.hasActiveEvent()) {
                worker.reduceEventDuration(intervalTicks);
                continue;
            }

            rollEvent(worker);
        }
    }

    private void rollEvent(Worker worker) {

        double morale = worker.morale();
        List<WorkerEventDefinition> candidates = new java.util.ArrayList<>();

        if (morale < lowMoraleThreshold) {
            candidates.addAll(eventManager.getByType(WorkerEventType.ILLNESS));
            candidates.addAll(eventManager.getByType(WorkerEventType.ACCIDENT));
            candidates.addAll(eventManager.getByType(WorkerEventType.STRIKE));
        }

        if (morale > HIGH_MORALE_THRESHOLD) {
            candidates.addAll(eventManager.getByType(WorkerEventType.PROMOTION));
        }

        if (worker.isEmployed()) {
            candidates.addAll(eventManager.getByType(WorkerEventType.VACATION));
        }

        for (WorkerEventDefinition definition : candidates) {

            double chance = definition.chance() > 0 ? definition.chance() : baseChance;

            if (random.nextDouble() < chance) {
                trigger(worker, definition);
                return;
            }
        }
    }

    private void trigger(Worker worker, WorkerEventDefinition definition) {

        worker.setHappiness(worker.happiness() + definition.happinessDelta());
        worker.setEnergy(worker.energy() + definition.energyDelta());
        worker.setHealth(worker.health() + definition.healthDelta());

        if (definition.type() != WorkerEventType.PROMOTION) {
            worker.triggerEvent(definition.id(), definition.durationTicks(), definition.workSpeedMultiplierWhileActive());
        }

        notifyEmployer(worker, definition);
    }

    private void notifyEmployer(Worker worker, WorkerEventDefinition definition) {

        Player employer = worker.employerId() != null ? Bukkit.getPlayer(worker.employerId()) : null;

        if (employer != null) {
            employer.sendMessage(Component.text("📋 Tu trabajador: " + definition.displayName(), NamedTextColor.GOLD));
        }
    }

}
