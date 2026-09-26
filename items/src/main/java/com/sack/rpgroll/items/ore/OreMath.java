package com.sack.rpgroll.items.ore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Cuentas de las menas sin tocar el servidor, para poder probarlas. */
public final class OreMath {

    /** Ticks que dura cada unidad de dureza: 30 si el bloque se puede recoger, 100 si no (como vanilla). */
    private static final double HARVESTABLE = 30.0;
    private static final double NOT_HARVESTABLE = 100.0;

    private static final int[][] DIRECTIONS = {
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

    private OreMath() {
    }

    /**
     * Por cuánto multiplicar la velocidad de picado para que el bloque
     * disfrazado ({@code currentSpeed}/{@code currentHardness}, lo que el
     * cliente cree que está picando) tarde lo que tardaría una mena de
     * {@code oreHardness} con una herramienta de {@code desiredSpeed}.
     */
    public static double breakSpeedMultiplier(double currentSpeed, double currentHardness, double desiredSpeed,
            double oreHardness, boolean harvestable) {

        double current = currentSpeed / currentHardness / HARVESTABLE;
        double desired = desiredSpeed / oreHardness / (harvestable ? HARVESTABLE : NOT_HARVESTABLE);
        return desired / current;
    }

    /** Velocidad de una herramienta con Eficiencia, como la suma vanilla (nivel² + 1). */
    public static double withEfficiency(double baseSpeed, int efficiency) {
        return efficiency > 0 ? baseSpeed + efficiency * efficiency + 1 : baseSpeed;
    }

    /** Fortuna como en las menas vanilla: multiplica por 1..(nivel+1), con más peso en 1. */
    public static int fortune(int base, int level, Random random) {

        if (level <= 0) {
            return base;
        }

        int bonus = random.nextInt(level + 2) - 1;
        return base * (bonus < 0 ? 1 : bonus + 1);
    }

    /**
     * Posiciones de una veta dentro de un chunk: crece bloque a bloque desde
     * el origen, sin salir del chunk (x/z en 0..15) ni de [minY, maxY].
     */
    public static List<int[]> vein(Random random, int x, int y, int z, int size, int minY, int maxY) {

        List<int[]> placed = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        add(placed, seen, x, clamp(y, minY, maxY), z);

        int attempts = size * 6;
        while (placed.size() < size && attempts-- > 0) {

            int[] from = placed.get(random.nextInt(placed.size()));
            int[] dir = DIRECTIONS[random.nextInt(DIRECTIONS.length)];

            int nx = from[0] + dir[0];
            int ny = from[1] + dir[1];
            int nz = from[2] + dir[2];

            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < minY || ny > maxY) {
                continue;
            }

            add(placed, seen, nx, ny, nz);
        }

        return placed;
    }

    /** Cuántas vetas toca en un chunk: la parte entera siempre, la decimal como probabilidad. */
    public static int veinCount(double veinsPerChunk, Random random) {
        int whole = (int) Math.floor(veinsPerChunk);
        return whole + (random.nextDouble() < veinsPerChunk - whole ? 1 : 0);
    }

    private static void add(List<int[]> placed, Set<Long> seen, int x, int y, int z) {
        long key = ((long) x << 40) ^ ((long) (y + 4096) << 16) ^ z;
        if (seen.add(key)) {
            placed.add(new int[] { x, y, z });
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}
