package com.sack.rpgroll.extras.temperature;

import java.util.List;

public record TemperatureSettings(double exchangeRate, int updateIntervalTicks, List<TemperatureStateRange> states) {

    public static TemperatureSettings defaults() {

        return new TemperatureSettings(0.05, 40, List.of(
                new TemperatureStateRange("severe_hypothermia", "Hipotermia severa", Double.NEGATIVE_INFINITY, 30,
                        List.of(new com.sack.rpgroll.extras.stat.PotionSpec("slowness", 2)), List.of()),
                new TemperatureStateRange("hypothermia", "Hipotermia", 30, 33,
                        List.of(new com.sack.rpgroll.extras.stat.PotionSpec("slowness", 0)), List.of()),
                new TemperatureStateRange("cold", "Frío", 33, 36, List.of(), List.of()),
                new TemperatureStateRange("normal", "Normal", 36, 39, List.of(), List.of()),
                new TemperatureStateRange("overheating", "Sobrecalentamiento", 39, 42,
                        List.of(new com.sack.rpgroll.extras.stat.PotionSpec("weakness", 0)), List.of()),
                new TemperatureStateRange("hyperthermia", "Hipertermia", 42, Double.POSITIVE_INFINITY,
                        List.of(new com.sack.rpgroll.extras.stat.PotionSpec("weakness", 1)), List.of())));
    }

}
