package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una variante evolutiva de una raza base (ej. Humano -&gt; Paladín). Al
 * evolucionar, el jugador conserva su raza base (identidad) pero suma los
 * bonos de la evolución activa: stats, traits, skills, afinidades,
 * resistencias/debilidades y profesiones exclusivas.
 */
public record RaceEvolution(
        String id,
        String baseRace,
        String displayName,
        AscensionRequirements requirements,
        Map<String, Double> statBonus,
        List<String> grantedTraits,
        List<String> grantedSkills,
        Map<String, Double> affinityBonus,
        Map<String, Double> resistances,
        Map<String, Double> weaknesses,
        List<String> unlockedProfessions) implements RPGContent {

    public RaceEvolution {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(baseRace, "baseRace no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        requirements = requirements == null ? AscensionRequirements.none() : requirements;
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
        grantedTraits = grantedTraits == null ? List.of() : List.copyOf(grantedTraits);
        grantedSkills = grantedSkills == null ? List.of() : List.copyOf(grantedSkills);
        affinityBonus = affinityBonus == null ? Map.of() : Map.copyOf(affinityBonus);
        resistances = resistances == null ? Map.of() : Map.copyOf(resistances);
        weaknesses = weaknesses == null ? Map.of() : Map.copyOf(weaknesses);
        unlockedProfessions = unlockedProfessions == null ? List.of() : List.copyOf(unlockedProfessions);
    }

}
