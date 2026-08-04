package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un rango evolutivo de un job (ej. Miner -&gt; Master Miner -&gt; Royal
 * Miner -&gt; Legendary Excavator). Puramente informativo por ahora — no
 * hay mecánica automática que lo otorgue; un admin o un addon (RPGRoll's
 * propio job system) puede consultarlo vía {@link JobEvolutionManager} y
 * decidir cuándo aplicarlo.
 */
public record JobEvolution(String id, String baseJob, String displayName, int requiredJobLevel,
        List<String> unlockedRecipes, List<String> unlockedTools, List<String> unlockedQuests)
        implements RPGContent {

    public JobEvolution {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(baseJob, "baseJob no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        unlockedRecipes = unlockedRecipes == null ? List.of() : List.copyOf(unlockedRecipes);
        unlockedTools = unlockedTools == null ? List.of() : List.copyOf(unlockedTools);
        unlockedQuests = unlockedQuests == null ? List.of() : List.copyOf(unlockedQuests);
    }

}
