package com.sack.rpgroll.crafting.quality;

/**
 * Calidad de un ítem que salió de una receta con {@code qualityEnabled=true}.
 * Mismo concepto/forma que {@code ProductQuality} de RPGRoll-Ranching, pero
 * para resultados de crafteo: un puntaje 0-100 (mezcla de nivel de job del
 * jugador y azar, ver {@link QualityRoller}) determina el tier.
 */
public enum CraftQuality {
    ROUGH("Tosca", "§7"),
    STANDARD("Estándar", "§f"),
    FINE("Fina", "§a"),
    MASTERWORK("Maestra", "§b"),
    LEGENDARY("Legendaria", "§6");

    private final String displayName;
    private final String colorCode;

    CraftQuality(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public static CraftQuality fromScore(double score) {

        if (score >= 95) {
            return LEGENDARY;
        }
        if (score >= 80) {
            return MASTERWORK;
        }
        if (score >= 55) {
            return FINE;
        }
        if (score >= 25) {
            return STANDARD;
        }
        return ROUGH;
    }

    public double valueMultiplier() {
        return switch (this) {
            case ROUGH -> 0.75;
            case STANDARD -> 1.0;
            case FINE -> 1.3;
            case MASTERWORK -> 1.7;
            case LEGENDARY -> 2.5;
        };
    }

    public String displayName() {
        return displayName;
    }

    public String colorCode() {
        return colorCode;
    }

    public String coloredLabel() {
        return colorCode + displayName;
    }

}
