package com.sack.rpgroll.extras.thermal;

/** {@code insulation}: reduce el intercambio térmico en general. {@code coldResistance}/{@code heatResistance}: reducen específicamente el frío o el calor (sección 8). */
public record ThermalProperties(double insulation, double coldResistance, double heatResistance) {

    public static final ThermalProperties NONE = new ThermalProperties(0, 0, 0);

    public ThermalProperties combine(ThermalProperties other) {
        return new ThermalProperties(
                clamp(insulation + other.insulation()),
                clamp(coldResistance + other.coldResistance()),
                clamp(heatResistance + other.heatResistance()));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

}
