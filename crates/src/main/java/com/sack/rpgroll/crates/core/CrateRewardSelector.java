package com.sack.rpgroll.crates.core;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sorteo ponderado: la probabilidad de cada CrateReward es su weight
 * dividido por la suma de weights del crate. El sorteo debe hacerse UNA
 * sola vez por apertura, antes de arrancar la animación — la animación
 * es puramente visual y nunca decide el resultado.
 */
public final class CrateRewardSelector {

    private CrateRewardSelector() {
    }

    public static CrateReward select(List<CrateReward> rewards) {

        double totalWeight = rewards.stream().mapToDouble(CrateReward::weight).sum();
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double cumulative = 0;

        for (CrateReward reward : rewards) {
            cumulative += reward.weight();
            if (roll <= cumulative) {
                return reward;
            }
        }

        // Solo alcanzable por redondeo de punto flotante — en la práctica
        // el for de arriba ya devolvió antes de llegar acá.
        return rewards.get(rewards.size() - 1);
    }

}
