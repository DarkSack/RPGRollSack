package com.sack.rpgroll.workers.core.economy;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Corre cada {@code wage-payment-interval-ticks}: cobra el salario de
 * los workers {@code HOURLY} empleados (los {@code PER_TASK} se cobran
 * al entregar en un almacén, ver {@code WorkerAiTask}), y chequea si
 * alguno, con la moral muy baja, decide renunciar.
 */
public class WageTask extends BukkitRunnable {

    private final WorkerManager workerManager;
    private final EconomyService economyService;
    private final MoraleEngine moraleEngine;
    private final long intervalTicks;
    private final LangManager lang;

    public WageTask(WorkerManager workerManager, EconomyService economyService, MoraleEngine moraleEngine,
            long intervalTicks, LangManager lang) {
        this.workerManager = workerManager;
        this.economyService = economyService;
        this.moraleEngine = moraleEngine;
        this.intervalTicks = intervalTicks;
        this.lang = lang;
    }

    @Override
    public void run() {

        for (Worker worker : List.copyOf(workerManager.getAll())) {

            if (!worker.isEmployed()) {
                continue;
            }

            if (worker.wageType() == com.sack.rpgroll.workers.core.economy.WageType.HOURLY) {

                worker.setWagePaymentRemainingTicks(worker.wagePaymentRemainingTicks() - intervalTicks);

                if (worker.wagePaymentRemainingTicks() <= 0) {

                    boolean paid = economyService.charge(worker.employerId(), worker.wageAmount());
                    worker.setWagePaymentRemainingTicks(intervalTicks);

                    if (paid) {
                        worker.setHappiness(worker.happiness() + 5);
                    } else {
                        worker.setHappiness(worker.happiness() - 10);
                        worker.setMotivation(worker.motivation() - 10);
                        notifyEmployer(worker, "task.wage.insufficient_funds");
                    }
                }
            }

            if (moraleEngine.shouldQuit(worker)) {
                notifyEmployer(worker, "task.wage.quit");
                worker.fire();
            }
        }
    }

    private void notifyEmployer(Worker worker, String key) {

        Player employer = worker.employerId() != null ? Bukkit.getPlayer(worker.employerId()) : null;

        if (employer != null) {
            employer.sendMessage(Component.text(lang.raw(key), NamedTextColor.YELLOW));
        }
    }

}
