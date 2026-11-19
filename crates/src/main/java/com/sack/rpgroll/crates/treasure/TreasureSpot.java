package com.sack.rpgroll.crates.treasure;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.random.RandomGenerator;

/** Elige el punto de un tesoro en el anillo de su {@link Treasure.Placement}. */
public final class TreasureSpot {

    private TreasureSpot() {
    }

    /**
     * Hasta {@code tries} puntos al azar en el anillo [minDistance, maxDistance] alrededor del origen; el
     * primero que acepte {@code valid} (bioma, mundo...).
     *
     * @return {x, z}, o vacío si ninguno vale
     */
    public static Optional<int[]> pick(int originX, int originZ, Treasure.Placement placement, RandomGenerator random,
                                       BiPredicate<Integer, Integer> valid, int tries) {

        double min = placement.minDistance();
        double max = placement.maxDistance();
        for (int i = 0; i < tries; i++) {
            // raíz para que el área del anillo se reparta igual (si no, se amontonan cerca del borde interior)
            double radius = Math.sqrt(min * min + random.nextDouble() * (max * max - min * min));
            double angle = random.nextDouble() * Math.PI * 2;
            int x = originX + (int) Math.round(Math.cos(angle) * radius);
            int z = originZ + (int) Math.round(Math.sin(angle) * radius);
            if (valid.test(x, z)) {
                return Optional.of(new int[]{x, z});
            }
        }
        return Optional.empty();
    }

}
