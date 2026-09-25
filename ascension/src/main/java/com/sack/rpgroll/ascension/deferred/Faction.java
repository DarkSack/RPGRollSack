package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Un reino, una orden o un gremio con el que se gana (o se pierde)
 * reputación. Ver {@link com.sack.rpgroll.ascension.engine.FactionEngine}.
 *
 * @param ranks        escalones, en cualquier orden (se ordenan por umbral)
 * @param sources      qué da reputación; el {@code amount} de cada criterio
 *                     es cuánta da cada vez
 * @param rivals       facciones que pierden reputación cuando se gana aquí
 * @param rivalPenalty qué fracción de lo ganado pierden las rivales (0.5 = la mitad)
 * @param min          suelo de la reputación
 * @param max          techo de la reputación
 */
public record Faction(String id, String displayName, List<FactionRank> ranks, List<Criterion> sources,
        List<String> rivals, double rivalPenalty, int min, int max) implements RPGContent {

    public Faction {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        ranks = ranks == null ? List.of()
                : ranks.stream().sorted(Comparator.comparingInt(FactionRank::threshold)).toList();
        sources = sources == null ? List.of() : List.copyOf(sources);
        rivals = rivals == null ? List.of() : List.copyOf(rivals);
        if (min > max) {
            throw new IllegalArgumentException("facción '" + id + "': min (" + min + ") mayor que max (" + max + ")");
        }
    }

    /** Una facción sin rangos ni fuentes: la reputación solo se da a mano. */
    public Faction(String id, String displayName) {
        this(id, displayName, List.of(), List.of(), List.of(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Faction withDisplayName(String value) {
        return new Faction(id, value, ranks, sources, rivals, rivalPenalty, min, max);
    }

    public int clamp(int reputation) {
        return Math.max(min, Math.min(max, reputation));
    }

    /** El rango más alto cuyo umbral no supera {@code reputation}. */
    public Optional<FactionRank> rankFor(int reputation) {

        FactionRank current = null;

        for (FactionRank rank : ranks) {
            if (rank.threshold() <= reputation) {
                current = rank;
            }
        }

        return Optional.ofNullable(current);
    }

    /** El siguiente rango por encima de {@code reputation}, si lo hay. */
    public Optional<FactionRank> nextRank(int reputation) {
        return ranks.stream().filter(rank -> rank.threshold() > reputation).findFirst();
    }

}
