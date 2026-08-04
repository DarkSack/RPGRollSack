package com.sack.rpgroll.enchantments.util;

/**
 * Convierte niveles de encantamiento a números romanos para el lore del
 * ítem (ej. nivel 4 -&gt; "IV"). Los encantamientos no tienen límite de
 * niveles, así que fuera del rango romano clásico se muestra el número
 * arábigo tal cual.
 */
public final class RomanNumerals {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumerals() {
    }

    public static String of(int number) {

        if (number < 1 || number > 3999) {
            return String.valueOf(number);
        }

        StringBuilder result = new StringBuilder();
        int remaining = number;

        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                remaining -= VALUES[i];
                result.append(SYMBOLS[i]);
            }
        }

        return result.toString();
    }

}
