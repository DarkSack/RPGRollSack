package com.sack.rpgroll.structures.storage;

/** Ítem que no cupo en el inventario y espera ser reclamado ({@code item} = {@code ItemStack#serializeAsBytes()}). */
public record PendingRow(long id, java.util.UUID player, byte[] item, long createdAt) {
}
