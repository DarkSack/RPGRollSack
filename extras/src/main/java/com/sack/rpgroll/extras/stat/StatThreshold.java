package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.extras.action.ExtrasAction;

import java.util.List;

/**
 * {@code comparison} se evalúa contra el valor ACTUAL del stat (ej. {@code "<=30"}).
 * {@code potions} se reaplica mientras la condición se mantenga verdadera
 * (como un potion effect vanilla de corta duración, refrescado en cada
 * tick de evaluación). {@code actions} se ejecuta UNA vez, al cruzar hacia
 * este umbral (no se repite mientras se mantiene). {@code applyConditions}
 * son ids de {@code ConditionDefinition} que se aplican mientras el umbral
 * se mantiene y se quitan al salir de él — es cómo un stat en 0 (ej. sed)
 * puede disparar daño PERIÓDICO reusando el motor de Conditions en vez de
 * reinventar "daño repetido" acá.
 */
public record StatThreshold(
        String comparison, List<PotionSpec> potions, List<ExtrasAction> actions, List<String> applyConditions) {
}
