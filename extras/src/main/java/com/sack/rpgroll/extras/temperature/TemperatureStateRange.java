package com.sack.rpgroll.extras.temperature;

import com.sack.rpgroll.extras.action.ExtrasAction;
import com.sack.rpgroll.extras.stat.PotionSpec;

import java.util.List;

/** Un tramo de temperatura corporal (sección 7) — {@code max} es exclusivo. */
public record TemperatureStateRange(
        String id, String label, double min, double max, List<PotionSpec> potions, List<ExtrasAction> actions) {

    public boolean matches(double bodyTemperature) {
        return bodyTemperature >= min && bodyTemperature < max;
    }

}
