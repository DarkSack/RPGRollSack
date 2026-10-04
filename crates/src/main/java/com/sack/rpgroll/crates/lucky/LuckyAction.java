package com.sack.rpgroll.crates.lucky;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Una acción de un resultado de lucky block: su tipo y sus parámetros tal
 * como vienen del YAML ({@code - {type: DROP, item: DIAMOND, amount: 2-4}}).
 * Las cantidades admiten un número o un rango {@code min-max}.
 */
public record LuckyAction(Type type, Map<String, String> params) {

    public enum Type {
        /** Suelta un ítem vanilla en el bloque: item, amount, name, enchantments, fake (no se puede coger). */
        DROP,
        /** Lluvia de ítems sobre el jugador: item, amount, radius, height. */
        RAIN,
        /** Comando de consola: command, con {player} {x} {y} {z} {world}. */
        COMMAND,
        /** Dinero por Vault: amount. */
        MONEY,
        /** Orbes de experiencia: amount. */
        XP,
        /** Otro lucky block: lucky (id), amount. */
        LUCKY,
        /** Mobs alrededor del bloque: entity, amount, name, baby. */
        MOB,
        /** Efecto de poción al jugador: effect, seconds, level. */
        POTION,
        /** Explosión que nunca rompe bloques: power, delay (ticks, con TNT encendida), fire. */
        EXPLOSION,
        /** Rayo: damage (vida que quita; el rayo en sí no quema nada). */
        LIGHTNING,
        /** Impulso hacia arriba: power. */
        LAUNCH,
        /** Jaula temporal alrededor del jugador, solo en el aire: block (que deje pasar la luz), seconds. */
        CAGE,
        /** Flechas que caen sobre el jugador: amount, height. */
        ARROWS,
        /** Fuegos artificiales: amount. */
        FIREWORK,
        /** Mensaje al jugador: text. */
        MESSAGE,
        /** Título al jugador: title, subtitle. */
        TITLE,
        /** Sonido en el bloque: sound, volume, pitch. */
        SOUND,
        /** Partículas en el bloque: particle, count. */
        PARTICLE
    }

    public LuckyAction {
        Objects.requireNonNull(type, "type");
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String text(String key, String fallback) {
        String value = params.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    public double number(String key, double fallback) {
        try {
            return Double.parseDouble(text(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean flag(String key, boolean fallback) {
        String value = params.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    /** "3" es 3; "2-5" es un número al azar entre 2 y 5, ambos incluidos. */
    public int amount(String key, int fallback, RandomGenerator random) {
        return roll(params.get(key), fallback, random);
    }

    static int roll(String raw, int fallback, RandomGenerator random) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        String value = raw.trim();
        int dash = value.indexOf('-', 1);

        try {
            if (dash < 0) {
                return Integer.parseInt(value);
            }
            int min = Integer.parseInt(value.substring(0, dash).trim());
            int max = Integer.parseInt(value.substring(dash + 1).trim());
            if (max < min) {
                int swap = min;
                min = max;
                max = swap;
            }
            return min + random.nextInt(max - min + 1);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static Type parseType(String raw) {
        return Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

}
