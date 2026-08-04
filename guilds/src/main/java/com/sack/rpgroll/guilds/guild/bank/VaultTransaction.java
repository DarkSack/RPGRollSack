package com.sack.rpgroll.guilds.guild.bank;

import java.util.UUID;

/** Entrada del historial del vault — spec: "historial/logs" de banco y almacén. */
public record VaultTransaction(long timestamp, UUID actorId, String actorName, VaultTransactionType type,
        double amount, String description) {

    public static VaultTransaction of(UUID actorId, String actorName, VaultTransactionType type, double amount,
            String description) {
        return new VaultTransaction(System.currentTimeMillis(), actorId, actorName, type, amount, description);
    }

}
