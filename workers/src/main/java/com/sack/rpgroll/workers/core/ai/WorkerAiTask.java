package com.sack.rpgroll.workers.core.ai;

import com.sack.rpgroll.workers.core.behavior.MovementUtil;
import com.sack.rpgroll.workers.core.behavior.ProfessionBehaviorRegistry;
import com.sack.rpgroll.workers.core.economy.EconomyService;
import com.sack.rpgroll.workers.core.economy.MoraleEngine;
import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.logistics.LogisticsEngine;
import com.sack.rpgroll.workers.core.logistics.Warehouse;
import com.sack.rpgroll.workers.core.logistics.WarehouseManager;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.schedule.Schedule;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Corre cada {@code ai-tick-interval-ticks}: para cada worker resuelve su
 * {@link AiAction} vía {@link AiEngine} y la ejecuta — mover con el
 * pathfinder vanilla, comer, depositar en un almacén (cobrando de paso
 * el salario {@code PER_TASK} si corresponde), o delegar el trabajo
 * físico real a un {@code ProfessionBehavior} registrado, con una
 * chance de éxito/error modulada por personalidad y moral.
 */
public class WorkerAiTask extends BukkitRunnable {

    private static final long NIGHT_START_TICK = 13000;
    private static final long NIGHT_END_TICK = 23000;
    private static final double BASE_ERROR_CHANCE = 0.05;

    private final WorkerManager workerManager;
    private final ProfessionManager professionManager;
    private final ScheduleManager scheduleManager;
    private final WarehouseManager warehouseManager;
    private final ProfessionBehaviorRegistry behaviorRegistry;
    private final EconomyService economyService;
    private final MoraleEngine moraleEngine;
    private final LogisticsEngine logisticsEngine = new LogisticsEngine();
    private final AiEngine aiEngine = new AiEngine();
    private final Random random = new Random();
    private final double warehouseSearchRadius;

    public WorkerAiTask(WorkerManager workerManager, ProfessionManager professionManager,
            ScheduleManager scheduleManager, WarehouseManager warehouseManager,
            ProfessionBehaviorRegistry behaviorRegistry, EconomyService economyService, MoraleEngine moraleEngine,
            double warehouseSearchRadius) {
        this.workerManager = workerManager;
        this.professionManager = professionManager;
        this.scheduleManager = scheduleManager;
        this.warehouseManager = warehouseManager;
        this.behaviorRegistry = behaviorRegistry;
        this.economyService = economyService;
        this.moraleEngine = moraleEngine;
        this.warehouseSearchRadius = warehouseSearchRadius;
    }

    @Override
    public void run() {

        for (Worker worker : List.copyOf(workerManager.getAll())) {

            Entity entity = Bukkit.getEntity(worker.id());

            if (!(entity instanceof LivingEntity living) || !living.isValid()) {
                continue;
            }

            Profession profession = professionManager.get(worker.professionId()).orElse(null);

            if (profession == null) {
                continue;
            }

            if (worker.hasActiveEvent() && worker.eventWorkSpeedMultiplier() <= 0) {
                worker.setCurrentAction(AiAction.IDLE);
                MovementUtil.stop(living);
                continue;
            }

            Schedule schedule = profession.scheduleId() != null
                    ? scheduleManager.get(profession.scheduleId()).orElse(null)
                    : null;

            World world = living.getWorld();
            WorldContext context = new WorldContext(world.hasStorm(), isNight(world.getTime()));

            AiAction action = aiEngine.decide(worker, profession, schedule, world.getFullTime(), context);
            worker.setCurrentAction(action);

            execute(worker, living, profession, action);
        }
    }

    private void execute(Worker worker, LivingEntity entity, Profession profession, AiAction action) {

        switch (action) {

            case SEEK_FOOD -> {
                if (worker.homeLocation() != null) {
                    MovementUtil.moveToward(entity, worker.homeLocation());
                }
            }

            // Autosuficiente: camina hacia el hogar si todavía no llegó, y come al llegar (o si no
            // tiene hogar asignado) — una sola regla "HUNGRY -> EAT" alcanza para todo el flujo
            // "buscar comida → comer → volver al trabajo" del diseño original.
            case EAT -> {
                if (worker.homeLocation() != null && entity.getLocation().distance(worker.homeLocation()) > 2.5) {
                    MovementUtil.moveToward(entity, worker.homeLocation());
                } else {
                    worker.setHunger(100);
                }
            }

            case GO_TO_WAREHOUSE -> findWarehouse(entity.getLocation())
                    .ifPresent(warehouse -> MovementUtil.moveToward(entity, warehouse.location()));

            case STORE_ITEMS -> findWarehouse(entity.getLocation()).ifPresent(warehouse -> {

                if (entity.getLocation().distance(warehouse.location()) <= 2.5) {

                    boolean hadItems = !worker.carriedItems().isEmpty();
                    logisticsEngine.deposit(worker, warehouse);

                    if (hadItems && worker.isEmployed() && worker.wageType() == WageType.PER_TASK) {
                        economyService.charge(worker.employerId(), worker.wageAmount());
                    }

                } else {
                    MovementUtil.moveToward(entity, warehouse.location());
                }
            });

            case SEEK_SHELTER, GO_HOME -> {
                if (worker.homeLocation() != null) {
                    MovementUtil.moveToward(entity, worker.homeLocation());
                } else {
                    MovementUtil.stop(entity);
                }
            }

            case SLEEP -> MovementUtil.stop(entity);

            case WORK -> performWork(worker, entity, profession);

            case IDLE -> MovementUtil.stop(entity);
        }
    }

    private void performWork(Worker worker, LivingEntity entity, Profession profession) {

        var behavior = behaviorRegistry.get(profession.id());

        if (behavior.isEmpty()) {
            return;
        }

        double speed = worker.personality().workSpeedMultiplier() * moraleEngine.workSpeedMultiplier(worker)
                * worker.eventWorkSpeedMultiplier();

        if (random.nextDouble() >= Math.min(1.0, speed)) {
            return;
        }

        double errorChance = BASE_ERROR_CHANCE * worker.personality().errorChanceMultiplier();

        if (random.nextDouble() < errorChance) {
            return;
        }

        behavior.get().work(worker, entity, profession);

        if (speed > 1.0 && random.nextDouble() < speed - 1.0) {
            behavior.get().work(worker, entity, profession);
        }
    }

    private Optional<Warehouse> findWarehouse(Location from) {
        return warehouseManager.findNearest(from, warehouseSearchRadius);
    }

    private boolean isNight(long time) {
        return time >= NIGHT_START_TICK && time < NIGHT_END_TICK;
    }

}
