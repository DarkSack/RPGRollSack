package com.sack.rpgroll.furniture.core;

/**
 * Un desplazamiento en bloques desde el bloque de un mueble, tal como queda con el mueble
 * mirando al sur (su frente hacia +Z). {@link #rotate(float)} lo lleva a la dirección en que
 * se colocó.
 */
public record Offset(double x, double y, double z) {

    public static final Offset ZERO = new Offset(0, 0, 0);

    /** "x,y,z" (o "x y z"). */
    public static Offset parse(String text) {

        String[] parts = text.trim().split("[,\\s]+");

        if (parts.length != 3) {
            throw new IllegalArgumentException("se esperaban tres números x,y,z: '" + text + "'");
        }

        return new Offset(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    /**
     * El mismo desplazamiento con el mueble mirando a {@code yaw} (grados de Minecraft: 0 sur,
     * 90 oeste, 180 norte, 270 este). Es el giro que el cliente aplica al ItemDisplay.
     */
    public Offset rotate(float yaw) {

        double rad = Math.toRadians(yaw);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        return new Offset(clean(x * cos - z * sin), y, clean(x * sin + z * cos));
    }

    /**
     * Igual que {@link #rotate} pero en casillas enteras. Con 8 o 16 direcciones una barrera no
     * puede girar 45°: se usa la dirección de 90° más cercana.
     */
    public int[] rotateBlock(float yaw) {

        float snapped = Math.round(yaw / 90f) * 90f;
        Offset r = rotate(snapped);
        return new int[] {(int) Math.round(r.x), (int) Math.round(r.y), (int) Math.round(r.z)};
    }

    private static double clean(double value) {
        return Math.abs(value) < 1e-9 ? 0 : value;
    }
}
