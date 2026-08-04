package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;
import java.util.Objects;

/**
 * Una escuela de magia (Fire, Holy, Necromancy...) — identidad visual más
 * afinidades. Las afinidades son fracciones (0.25 = +25%, -0.20 = -20%)
 * aplicadas como multiplicador extra sobre el daño/curación de cualquier
 * hechizo de esta escuela — {@code raceAffinities}/{@code classAffinities}
 * mapean id de raza/clase (de RPGRollAPI) a esa fracción.
 * <p>
 * No hay jerarquía real entre escuelas (ej. "Elemental" como padre de
 * "Fire"/"Water") — es puramente organizativo, así que no se modela: cada
 * escuela es un id plano y cualquier addon puede agrupar varias bajo un
 * mismo tag si quiere.
 */
public record MagicSchool(
        String id,
        String displayName,
        String color,
        String icon,
        String description,
        String castSoundOnCast,
        String castEffectId,
        Map<String, Double> raceAffinities,
        Map<String, Double> classAffinities) implements RPGContent {

    public MagicSchool {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        color = color == null || color.isBlank() ? "WHITE" : color;
        icon = icon == null || icon.isBlank() ? "BOOK" : icon;
        description = description == null ? "" : description;
        castSoundOnCast = (castSoundOnCast == null || castSoundOnCast.isBlank()) ? null : castSoundOnCast;
        castEffectId = (castEffectId == null || castEffectId.isBlank()) ? null : castEffectId;
        raceAffinities = raceAffinities == null ? Map.of() : Map.copyOf(raceAffinities);
        classAffinities = classAffinities == null ? Map.of() : Map.copyOf(classAffinities);
    }

}
