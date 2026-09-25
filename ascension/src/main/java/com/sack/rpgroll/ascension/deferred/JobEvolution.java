package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.reward.Rewards;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un rango de un oficio (Minero → Maestro Minero → …). Se obtiene solo al
 * llegar a {@code requiredJobLevel} en {@code baseJob}; ver
 * {@link com.sack.rpgroll.ascension.engine.JobEvolutionEngine}.
 *
 * @param unlockedRecipes da el permiso {@code rpgrollascension.recipe.<id>};
 *                        una receta de RPGRoll-Crafting lo exige con una
 *                        condición {@code PERMISSION}
 * @param unlockedTools   ids de RPGRoll-Items que se entregan al evolucionar
 * @param unlockedQuests  da el permiso {@code rpgrollascension.quest.<id>}
 */
public record JobEvolution(String id, String baseJob, String displayName, int requiredJobLevel,
        List<String> unlockedRecipes, List<String> unlockedTools, List<String> unlockedQuests, Rewards rewards)
        implements RPGContent {

    public JobEvolution {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(baseJob, "baseJob no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        unlockedRecipes = unlockedRecipes == null ? List.of() : List.copyOf(unlockedRecipes);
        unlockedTools = unlockedTools == null ? List.of() : List.copyOf(unlockedTools);
        unlockedQuests = unlockedQuests == null ? List.of() : List.copyOf(unlockedQuests);
        rewards = rewards == null ? Rewards.none() : rewards;
    }

    public JobEvolution(String id, String baseJob, String displayName, int requiredJobLevel,
            List<String> unlockedRecipes, List<String> unlockedTools, List<String> unlockedQuests) {
        this(id, baseJob, displayName, requiredJobLevel, unlockedRecipes, unlockedTools, unlockedQuests,
                Rewards.none());
    }

}
