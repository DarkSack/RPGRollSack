package com.sack.rpgroll.pass.season;

import com.sack.rpgroll.pass.reward.Reward;

import java.util.List;

/** Lo que da un nivel del pase en cada pista. Una pista vacía no tiene nada que reclamar. */
public record SeasonLevel(int level, List<Reward> free, List<Reward> premium) {
}
