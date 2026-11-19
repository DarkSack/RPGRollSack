package com.sack.rpgroll.crates.treasure;

/** La pista del mapa: hacia dónde y a cuántos bloques queda el tesoro. */
public final class TreasureHint {

    private static final String[] DIRECTIONS = {"n", "ne", "e", "se", "s", "so", "o", "no"};

    private TreasureHint() {
    }

    /** Clave de lang del rumbo (n, ne, e, se, s, so, o, no); el norte es -z, como en Minecraft. */
    public static String direction(int dx, int dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        int sector = (int) Math.floorMod(Math.round(degrees / 45.0), 8);
        return DIRECTIONS[sector];
    }

    /** Distancia horizontal en bloques, redondeada. */
    public static int distance(int dx, int dz) {
        return (int) Math.round(Math.hypot(dx, dz));
    }

}
