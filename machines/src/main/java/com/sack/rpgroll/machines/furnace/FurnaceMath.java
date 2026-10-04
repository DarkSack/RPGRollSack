package com.sack.rpgroll.machines.furnace;

/**
 * Las cuentas de un horno mejorado, sin Bukkit (para probarlas).
 * <p>
 * Un horno más rápido quema el combustible al mismo ritmo por tick, así que sin corregir nada
 * cocería {@code speed} veces más ítems con cada carbón. El tiempo de quemado se divide por
 * la velocidad: el combustible rinde lo mismo por ítem que en vanilla, y lo que lo mejora es
 * {@code fuel} (1.25 = un 25 % más de ítems por carbón).
 */
public final class FurnaceMath {

    private FurnaceMath() {
    }

    /** Ticks que tarda un ítem: nunca menos de 1. */
    public static int cookTime(int base, double speed) {
        return Math.max(1, (int) Math.round(base / speed));
    }

    /** Ticks que dura un combustible. 0 se queda en 0 (no es combustible). */
    public static int burnTime(int base, double speed, double fuel) {
        if (base <= 0) {
            return base;
        }
        return Math.max(1, (int) Math.round(base / speed * fuel));
    }

    /**
     * Cuántos ítems salen: el doble si toca y caben en la casilla de resultado (si no, vanilla:
     * el horno ya comprobó que cabía uno).
     */
    public static int result(int amount, int inSlot, int maxStack, double chance, double roll) {
        if (chance <= 0 || roll >= chance) {
            return amount;
        }
        return inSlot + amount * 2 <= maxStack ? amount * 2 : amount;
    }
}
