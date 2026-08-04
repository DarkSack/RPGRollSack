package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * @param symptoms                 tags puramente descriptivos, mostrados en el Ranch Studio y la enciclopedia
 * @param contagionChance          0-1, evaluada por chequeo periódico contra animales sanos cercanos
 * @param healthPenaltyPerCheck    cuánto baja la salud por cada chequeo periódico mientras está enferma
 * @param happinessPenaltyPerCheck cuánto baja la felicidad por cada chequeo
 * @param productionPenalty        multiplicador (0-1) sobre la producción mientras está enferma
 */
public record Disease(
        String id,
        String displayName,
        String description,
        Set<String> symptoms,
        long durationTicks,
        double contagionChance,
        double healthPenaltyPerCheck,
        double happinessPenaltyPerCheck,
        double productionPenalty) implements RPGContent {

    public Disease {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        symptoms = symptoms == null ? Set.of() : Set.copyOf(symptoms);
        durationTicks = Math.max(1, durationTicks);
        contagionChance = Math.max(0, Math.min(1, contagionChance));
        healthPenaltyPerCheck = Math.max(0, healthPenaltyPerCheck);
        happinessPenaltyPerCheck = Math.max(0, happinessPenaltyPerCheck);
        productionPenalty = Math.max(0, Math.min(1, productionPenalty));
    }

}
