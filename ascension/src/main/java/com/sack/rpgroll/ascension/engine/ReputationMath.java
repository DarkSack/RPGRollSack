package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Faction;
import com.sack.rpgroll.ascension.deferred.FactionRank;

import java.util.List;
import java.util.Optional;

/** Las cuentas de la reputación, sin nada de Bukkit. Ver {@link FactionEngine}. */
public final class ReputationMath {

    private ReputationMath() {
    }

    /** Lo que pierde una facción rival cuando se ganan {@code gained} puntos aquí. */
    public static int rivalLoss(Faction faction, int gained) {
        return gained <= 0 ? 0 : (int) Math.round(gained * faction.rivalPenalty());
    }

    /**
     * Rangos en los que se entra con un cambio de {@code oldReputation} a
     * {@code newReputation}. Subiendo, todos los que se cruzan, en orden: si
     * un admin da 5000 de golpe, no se salta las recompensas intermedias.
     * Bajando, solo el rango en el que se cae.
     */
    public static List<FactionRank> ranksEntered(Faction faction, int oldReputation, int newReputation) {

        if (newReputation > oldReputation) {
            return faction.ranks().stream()
                    .filter(rank -> rank.threshold() > oldReputation && rank.threshold() <= newReputation)
                    .toList();
        }

        if (newReputation < oldReputation) {
            Optional<FactionRank> before = faction.rankFor(oldReputation);
            Optional<FactionRank> after = faction.rankFor(newReputation);
            if (after.isPresent() && !after.equals(before)) {
                return List.of(after.get());
            }
        }

        return List.of();
    }

}
