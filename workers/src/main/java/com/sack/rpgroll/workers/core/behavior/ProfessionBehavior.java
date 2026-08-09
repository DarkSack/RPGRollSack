package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.entity.LivingEntity;

/**
 * Qué hace físicamente un worker mientras su acción de IA es
 * {@code WORK} — un tick de "trabajar de verdad" para una profesión
 * puntual (romper una veta, cosechar un cultivo, cuidar un animal de
 * Ranching...). Registrable vía {@code WorkersAPI.registerBehavior(...)}
 * para que un addon (o un admin, indirectamente) le dé comportamiento
 * real a una profesión custom sin tocar el código de este módulo.
 * <p>
 * Una profesión sin behavior registrado sigue "trabajando" a efectos de
 * horario/salario/experiencia — solo que sin ninguna interacción física
 * real con el mundo (fallback silencioso, no un error).
 */
@FunctionalInterface
public interface ProfessionBehavior {

    void work(Worker worker, LivingEntity entity, Profession profession);

}
