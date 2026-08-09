package com.sack.rpgroll.economy.ledger;

import java.util.UUID;

/**
 * Un movimiento económico ya ocurrido, tal como queda escrito en el libro
 * mayor. {@code actorId} es {@code null} para movimientos del sistema (ej.
 * un sink o el recálculo de intereses) que no pertenecen a un jugador.
 */
public record TransactionRecord(
        long id,
        long timestampMillis,
        UUID actorId,
        TransactionType type,
        String currencyId,
        double amount,
        double balanceAfter,
        String description) {
}
