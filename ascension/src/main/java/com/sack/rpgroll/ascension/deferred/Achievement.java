package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.reward.Rewards;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un logro. Se desbloquea solo cuando se cumplen todos sus
 * {@code criteria} (ver {@link com.sack.rpgroll.ascension.engine.AchievementEngine}),
 * o a mano con {@code /ascendadmin achievement grant}. Sin criterios, solo
 * se puede otorgar a mano.
 *
 * @param hidden no se muestra en {@code /ascend achievements} hasta desbloquearlo
 */
public record Achievement(String id, String displayName, String description, boolean hidden,
        List<Criterion> criteria, Rewards rewards) implements RPGContent {

    public Achievement {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        rewards = rewards == null ? Rewards.none() : rewards;
    }

    /** Un logro sin criterios ni recompensas: solo se otorga a mano. */
    public Achievement(String id, String displayName, String description) {
        this(id, displayName, description, false, List.of(), Rewards.none());
    }

    public Achievement withDisplayName(String value) {
        return new Achievement(id, value, description, hidden, criteria, rewards);
    }

    public Achievement withDescription(String value) {
        return new Achievement(id, displayName, value, hidden, criteria, rewards);
    }

}
