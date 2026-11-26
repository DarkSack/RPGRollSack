package com.sack.rpgroll.structures.storage;

import java.util.UUID;

/** Dinero que espera pagarse a un jugador (Vault no estaba o falló); {@code amountCents} en céntimos. */
public record PendingMoneyRow(long id, UUID player, long amountCents, long createdAt) {
}
