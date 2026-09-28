package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.DepthRequirement;
import com.sack.rpgroll.fishing.core.TimeRequirement;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.fishing.core.WeatherType;

import java.util.Set;

/**
 * Todo lo que importa del momento/lugar de una picada — resuelto una sola vez por {@link FishingConditionsResolver}.
 *
 * @param depths las capas que hay en la columna de agua del anzuelo (la boya siempre flota arriba: lo que
 *               cuenta es qué capas existen ahí, no dónde queda el anzuelo)
 */
public record FishingConditions(
        String biome,
        WaterType waterType,
        Set<DepthRequirement> depths,
        WeatherType weather,
        Set<TimeRequirement> activeTimes,
        String seasonId) {
}
