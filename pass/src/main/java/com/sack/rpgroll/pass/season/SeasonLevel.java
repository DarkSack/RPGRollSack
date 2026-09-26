package com.sack.rpgroll.pass.season;

import com.sack.rpgroll.pass.requirement.LevelRequirements;
import com.sack.rpgroll.pass.reward.Reward;

import java.util.List;

/**
 * Lo que da un nivel del pase en cada pista y lo que exige, además de haber
 * llegado a él, para reclamarlo. Una pista vacía no tiene nada que reclamar.
 */
public record SeasonLevel(int level, List<Reward> free, List<Reward> premium, LevelRequirements requirements) {

    public SeasonLevel {
        requirements = requirements == null ? LevelRequirements.NONE : requirements;
    }

    public SeasonLevel(int level, List<Reward> free, List<Reward> premium) {
        this(level, free, premium, LevelRequirements.NONE);
    }

}
