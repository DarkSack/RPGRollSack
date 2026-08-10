package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;

import java.util.Locale;

/**
 * Mini-DSL de una línea para escribir un {@link IngredientSpec} por chat:
 * {@code TIPO VALOR CANTIDAD [CALIDAD-MINIMA]}, ej.
 * {@code MATERIAL IRON_INGOT 2} o {@code ITEM_ID enchanted_gem 1 FINE}.
 * {@code ANY} no necesita valor: {@code ANY 1}.
 */
public final class IngredientSpecFormat {

    private IngredientSpecFormat() {
    }

    /** @throws IllegalArgumentException con un mensaje apto para mostrarle al jugador si el texto no es válido. */
    public static IngredientSpec parse(String raw) {

        String[] tokens = raw.trim().split("\\s+");

        if (tokens.length < 2) {
            throw new IllegalArgumentException("Formato: TIPO VALOR CANTIDAD [CALIDAD-MINIMA]");
        }

        IngredientType type;
        try {
            type = IngredientType.valueOf(tokens[0].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo inválido: " + tokens[0]);
        }

        String value;
        int amountIndex;

        if (type == IngredientType.ANY) {
            value = null;
            amountIndex = 1;
        } else {
            if (tokens.length < 3) {
                throw new IllegalArgumentException("Formato: TIPO VALOR CANTIDAD [CALIDAD-MINIMA]");
            }
            value = tokens[1];
            amountIndex = 2;
        }

        int amount;
        try {
            amount = Integer.parseInt(tokens[amountIndex]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cantidad inválida: " + tokens[amountIndex]);
        }

        String minQuality = tokens.length > amountIndex + 1 ? tokens[amountIndex + 1] : null;

        return new IngredientSpec(type, value, amount, minQuality);
    }

    public static String format(IngredientSpec spec) {

        StringBuilder builder = new StringBuilder(spec.type().name());

        if (spec.value() != null) {
            builder.append(' ').append(spec.value());
        }

        builder.append(" x").append(spec.amount());

        if (spec.hasMinQuality()) {
            builder.append(" (min: ").append(spec.minQuality()).append(')');
        }

        return builder.toString();
    }

}
