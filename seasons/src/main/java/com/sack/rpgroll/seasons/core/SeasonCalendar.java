package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un calendario — un ciclo ordenado de ids de {@link Season} que se repite
 * indefinidamente (al llegar al final vuelve al principio y suma un año).
 * No hace falta que se llame "año/primavera/verano..." — puede ser
 * cualquier ciclo temático ("Luna Roja" → "Era del Sol" → "Oscuridad" →
 * "Renacimiento"). Se llama {@code SeasonCalendar} y no simplemente
 * {@code Calendar} para no confundirse/colisionar con {@code
 * java.util.Calendar}.
 */
public record SeasonCalendar(String id, String displayName, String description, List<String> seasonIds)
        implements RPGContent {

    public SeasonCalendar {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        seasonIds = seasonIds == null ? List.of() : List.copyOf(seasonIds);
    }

}
