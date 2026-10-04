package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Un tipo de lucky block (plugins/RPGRoll-Crates/lucky/*.yml).
 * <p>
 * En el mundo es un bloque musical con instrumento {@code skeleton} y la nota
 * {@code note}: el resource pack dibuja cada nota como un lucky block
 * distinto, igual que las menas de RPGRoll-Items con el instrumento
 * {@code zombie}. Como ítem es {@code material} con {@code itemModel}.
 *
 * @param itemModel modelo del ítem ({@code sackito:lucky_comun}), o null para el material tal cual
 */
public record LuckyBlock(
        String id,
        String displayName,
        List<String> lore,
        int note,
        String material,
        String itemModel,
        boolean glow,
        List<LuckyOutcome> outcomes) implements RPGContent {

    public LuckyBlock {
        Objects.requireNonNull(id, "id");
        if (note < 0 || note > 24) {
            throw new IllegalArgumentException("note debe ir de 0 a 24 (lucky block '" + id + "')");
        }
        displayName = displayName == null ? id : displayName;
        lore = lore == null ? List.of() : List.copyOf(lore);
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }

    public double totalWeight() {
        return outcomes.stream().mapToDouble(LuckyOutcome::weight).sum();
    }

    /** Un resultado al azar según su weight, o null si no tiene ninguno. */
    public LuckyOutcome roll(RandomGenerator random) {

        double total = totalWeight();
        if (total <= 0) {
            return null;
        }

        double pick = random.nextDouble() * total;
        for (LuckyOutcome outcome : outcomes) {
            pick -= outcome.weight();
            if (pick < 0) {
                return outcome;
            }
        }
        return outcomes.get(outcomes.size() - 1);
    }

    public LuckyOutcome outcome(String outcomeId) {
        return outcomes.stream().filter(o -> o.id().equalsIgnoreCase(outcomeId)).findFirst().orElse(null);
    }

}
