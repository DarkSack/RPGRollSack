package com.sack.rpgroll.economy.wallet;

/** Resultado de una operación de wallet — evita excepciones para el flujo normal de "no alcanza el saldo". */
public enum EconomyResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    LOCKED,
    UNKNOWN_CURRENCY,
    INVALID_AMOUNT,
    LIMIT_EXCEEDED
}
