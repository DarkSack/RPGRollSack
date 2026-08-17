package com.sack.rpgroll.crafting.discovery;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

/** Resultado de un intento de {@code /crafting experiment}. {@code recipe} solo está presente si result es DISCOVERED. */
public record ExperimentationOutcome(ExperimentationResult result, CustomRecipe recipe) {

    public static ExperimentationOutcome of(ExperimentationResult result) {
        return new ExperimentationOutcome(result, null);
    }

    public static ExperimentationOutcome discovered(CustomRecipe recipe) {
        return new ExperimentationOutcome(ExperimentationResult.DISCOVERED, recipe);
    }

}
