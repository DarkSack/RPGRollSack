package com.sack.rpgroll.workers.api;

import com.sack.rpgroll.workers.core.behavior.ProfessionBehavior;
import com.sack.rpgroll.workers.core.behavior.ProfessionBehaviorRegistry;
import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.logistics.WarehouseManager;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.workers.core.skill.SkillManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Punto de entrada público de RPGRoll-Workers. Expone los managers de
 * contenido para registrar profesiones/habilidades/horarios/eventos por
 * código, {@link #registerBehavior} para darle comportamiento físico
 * real a una profesión custom, y consultas de workers pensadas en
 * primer lugar para un futuro objetivo de RPGRoll-Quests ("contratá 5
 * trabajadores") — hoy esa consulta ya funciona, solo falta que Quests
 * defina el tipo de objetivo que la llame.
 */
public final class WorkersAPI {

    private static WorkersAPI instance;

    private final ProfessionManager professionManager;
    private final SkillManager skillManager;
    private final ScheduleManager scheduleManager;
    private final WorkerEventManager workerEventManager;
    private final WorkerManager workerManager;
    private final WarehouseManager warehouseManager;
    private final ProfessionBehaviorRegistry behaviorRegistry;

    private WorkersAPI(ProfessionManager professionManager, SkillManager skillManager, ScheduleManager scheduleManager,
            WorkerEventManager workerEventManager, WorkerManager workerManager, WarehouseManager warehouseManager,
            ProfessionBehaviorRegistry behaviorRegistry) {
        this.professionManager = professionManager;
        this.skillManager = skillManager;
        this.scheduleManager = scheduleManager;
        this.workerEventManager = workerEventManager;
        this.workerManager = workerManager;
        this.warehouseManager = warehouseManager;
        this.behaviorRegistry = behaviorRegistry;
    }

    public static void init(ProfessionManager professionManager, SkillManager skillManager,
            ScheduleManager scheduleManager, WorkerEventManager workerEventManager, WorkerManager workerManager,
            WarehouseManager warehouseManager, ProfessionBehaviorRegistry behaviorRegistry) {
        instance = new WorkersAPI(professionManager, skillManager, scheduleManager, workerEventManager, workerManager,
                warehouseManager, behaviorRegistry);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Workers todavía no está listo. */
    public static WorkersAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Workers todavía no está listo.");
        }

        return instance;
    }

    // ============ Managers de contenido (registerX por código) ============

    public ProfessionManager professions() {
        return professionManager;
    }

    public SkillManager skills() {
        return skillManager;
    }

    public ScheduleManager schedules() {
        return scheduleManager;
    }

    public WorkerEventManager workerEvents() {
        return workerEventManager;
    }

    public WarehouseManager warehouses() {
        return warehouseManager;
    }

    /** Le da comportamiento físico real a una profesión — ver {@link ProfessionBehavior}. */
    public void registerBehavior(String professionId, ProfessionBehavior behavior) {
        behaviorRegistry.register(professionId, behavior);
    }

    // ============ Consulta de workers ============

    public Optional<Worker> getWorker(Entity entity) {
        return workerManager.resolve(entity);
    }

    public Collection<Worker> getAllWorkers() {
        return workerManager.getAll();
    }

    /** Pensado en primer lugar para un futuro objetivo de RPGRoll-Quests ("contratá N trabajadores"). */
    public List<Worker> getEmployedWorkers(UUID employerId) {
        return workerManager.getAll().stream().filter(worker -> employerId.equals(worker.employerId())).toList();
    }

}
