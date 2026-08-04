package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Un evento mundial — lluvia de meteoritos, eclipse, aurora, tormenta
 * mágica... Se dispara sobre un mundo entero (todos los jugadores online
 * en él), a mano ({@code /seasonsadmin trigger}) o automáticamente (una
 * {@link Season} lo sortea entre sus {@code worldEventIds} una vez por
 * día de Minecraft).
 *
 * @param durationTicks cuánto dura activo (afecta principalmente a SET_WEATHER, que se revierte al terminar)
 */
public record WorldEvent(String id, String displayName, String icon, String description, int durationTicks,
        List<WorldEventComponent> components) implements RPGContent {

    public WorldEvent {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "NETHER_STAR" : icon;
        description = description == null ? "" : description;
        durationTicks = Math.max(0, durationTicks);
        components = components == null ? List.of() : List.copyOf(components);
    }

}
