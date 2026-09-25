package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.reward.Rewards;

import java.util.Objects;

/**
 * Un escalón de reputación con una facción. Sus recompensas se entregan una
 * sola vez, la primera vez que se alcanza.
 *
 * @param threshold reputación mínima para estar en este rango (puede ser negativa)
 */
public record FactionRank(String id, String displayName, int threshold, Rewards rewards) {

    public FactionRank {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        rewards = rewards == null ? Rewards.none() : rewards;
    }

}
