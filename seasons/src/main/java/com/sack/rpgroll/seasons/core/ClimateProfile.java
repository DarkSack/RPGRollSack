package com.sack.rpgroll.seasons.core;

/**
 * El clima que impone una {@link Season} sobre cada mundo donde está
 * activa. Las chances son fracciones 0.0-1.0, re-rolladas periódicamente
 * por el motor (no un estado fijo) — así el clima varía dentro de la
 * misma estación en vez de ser todo lluvia o nada.
 *
 * @param baseTemperature   temperatura base en °C que se suma al valor propio del bioma
 * @param temperatureVariance variación aleatoria +/- aplicada sobre la temperatura resultante
 */
public record ClimateProfile(
        double rainChance,
        double stormChance,
        double snowChance,
        double fogChance,
        double baseTemperature,
        double temperatureVariance,
        double windStrength,
        double humidity,
        double heatwaveChance,
        double thunderstormChance) {

    public ClimateProfile {
        rainChance = clamp(rainChance);
        stormChance = clamp(stormChance);
        snowChance = clamp(snowChance);
        fogChance = clamp(fogChance);
        windStrength = clamp(windStrength);
        humidity = clamp(humidity);
        heatwaveChance = clamp(heatwaveChance);
        thunderstormChance = clamp(thunderstormChance);
    }

    public static ClimateProfile temperate() {
        return new ClimateProfile(0.3, 0.05, 0.0, 0.1, 15.0, 3.0, 0.3, 0.5, 0.02, 0.02);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

}
