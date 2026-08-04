package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un rango de prestigio (I, II, III...). {@code id} es el número de
 * prestigio como texto (ej. "1"). Al alcanzarlo, el jugador vuelve a nivel
 * 1 y obtiene {@code expBonusPercent} de forma ACUMULATIVA con los
 * prestigios anteriores, más las habilidades que otorgue.
 */
public record PrestigeLevel(String id, int requiredLevel, double expBonusPercent, List<String> grantedSkills)
        implements RPGContent {

    public PrestigeLevel {
        Objects.requireNonNull(id, "id no puede ser null");
        grantedSkills = grantedSkills == null ? List.of() : List.copyOf(grantedSkills);
    }

    public int number() {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
