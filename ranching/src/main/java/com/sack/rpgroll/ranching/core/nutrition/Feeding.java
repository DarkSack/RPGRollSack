package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.species.Species;

/**
 * Dar de comer a un animal — el mismo efecto desde la mano del jugador que desde la API. El
 * {@code nutrition-value} del alimento llena la saciedad del animal: lleno, no come (y el ítem no se
 * gasta), así que no se puede atiborrar a un animal para disparar su próxima producción.
 */
public final class Feeding {

    public enum Result {
        FED,
        /** Comió, pero no es de su dieta: rinde menos. */
        FED_OFF_DIET,
        /** Está lleno: no comió. */
        FULL
    }

    /** Lo que rinde un alimento fuera de la dieta de la especie. */
    static final double OFF_DIET_EFFECTIVENESS = 0.4;

    private Feeding() {
    }

    public static Result feed(Animal animal, Feed feed, Species species) {

        if (animal.isFull()) {
            return Result.FULL;
        }

        boolean matchesDiet = species == null || feed.satisfiesDiet(species.dietTags());
        double effectiveness = matchesDiet ? 1.0 : OFF_DIET_EFFECTIVENESS;

        animal.eat(feed.nutritionValue());
        animal.setHealth(animal.health() + feed.healthBonus() * effectiveness);
        animal.setHappiness(animal.happiness() + feed.happinessBonus() * effectiveness);
        animal.addProductionBonus(feed.productionBonus() * feed.quality().multiplier() * effectiveness);

        return matchesDiet ? Result.FED : Result.FED_OFF_DIET;
    }

}
