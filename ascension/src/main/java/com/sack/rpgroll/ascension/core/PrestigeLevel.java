package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un rango de prestigio (I, II, III...). {@code id} es el número de
 * prestigio como texto (ej. "1"). Al alcanzarlo, el jugador vuelve a nivel
 * 1 y obtiene {@code expBonusPercent} de forma ACUMULATIVA con los
 * prestigios anteriores, más las habilidades que otorgue.
 * <p>
 * {@code requirements} son condiciones extra además del nivel, con el mismo
 * formato que las evoluciones y especializaciones (por ejemplo una quest de
 * prueba en {@code completed-quests}). Opcionales.
 */
public record PrestigeLevel(String id, int requiredLevel, double expBonusPercent, List<String> grantedSkills,
        AscensionRequirements requirements) implements RPGContent {

    public PrestigeLevel {
        Objects.requireNonNull(id, "id no puede ser null");
        grantedSkills = grantedSkills == null ? List.of() : List.copyOf(grantedSkills);
        requirements = requirements == null ? AscensionRequirements.none() : requirements;
    }

    public PrestigeLevel(String id, int requiredLevel, double expBonusPercent, List<String> grantedSkills) {
        this(id, requiredLevel, expBonusPercent, grantedSkills, AscensionRequirements.none());
    }

    /** Copia con otros valores editables, conservando los requisitos. */
    public PrestigeLevel with(int newRequiredLevel, double newExpBonusPercent, List<String> newSkills) {
        return new PrestigeLevel(id, newRequiredLevel, newExpBonusPercent, newSkills, requirements);
    }

    public int number() {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
