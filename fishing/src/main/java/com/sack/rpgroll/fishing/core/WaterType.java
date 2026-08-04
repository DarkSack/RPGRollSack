package com.sack.rpgroll.fishing.core;

/**
 * Dónde vive un pez. Se resuelve automáticamente a partir del bioma/bloque
 * del anzuelo — salvo {@code MAGIC_WATER}/{@code CORRUPTED_WATER}, que
 * solo existen dentro de una {@link FishingRegion} que las fuerce (no hay
 * ningún bioma vanilla que las implique).
 * <p>
 * {@code LAVA} está reservado para una futura versión con boya propia:
 * este addon reutiliza {@code PlayerFishEvent} de Bukkit tal cual (no
 * reimplementa el cast/espera/mordida), y vanilla nunca genera una mordida
 * con el anzuelo flotando en lava — una especie que requiera {@code LAVA}
 * en {@code water-types} hoy sería, en la práctica, imposible de pescar.
 */
public enum WaterType {
    RIVER,
    LAKE,
    SWAMP,
    OCEAN,
    DEEP_OCEAN,
    LAVA,
    MAGIC_WATER,
    CORRUPTED_WATER
}
