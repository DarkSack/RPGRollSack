package com.sack.rpgroll.fishing.engine;

import com.sack.rpgroll.fishing.core.DepthRequirement;
import com.sack.rpgroll.fishing.core.TimeRequirement;
import com.sack.rpgroll.fishing.core.WaterType;
import com.sack.rpgroll.fishing.core.WeatherType;

import java.util.Set;

/** Todo lo que importa del momento/lugar de una picada — resuelto una sola vez por {@link FishingConditionsResolver}. */
public record FishingConditions(
        String biome,
        WaterType waterType,
        DepthRequirement depth,
        WeatherType weather,
        Set<TimeRequirement> activeTimes,
        String seasonId) {
}
