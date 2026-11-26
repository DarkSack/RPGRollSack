package com.sack.rpgroll.structures.storage;

import java.util.UUID;

/**
 * Un apunte del libro de materiales. Su {@code id} lo hace idempotente: aplicarlo dos veces cuenta una.
 * {@code MONEY_CHARGE} y {@code MONEY_REFUND} van con {@link #MONEY_KEY} y la cantidad en céntimos; solo dejan rastro
 * en el libro (el dinero lo mueve Vault).
 */
public record Movement(UUID id, UUID project, UUID player, Kind kind, String materialKey, int amount) {

    /** Material de los apuntes de dinero (sin «:», no choca con ningún material). */
    public static final String MONEY_KEY = "money";

    public enum Kind { DEPOSIT, WITHDRAW, REFUND, CONSUME, CLAIM, MONEY_CHARGE, MONEY_REFUND }
}
