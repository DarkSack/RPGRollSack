package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.common.structure.Rotation;

import java.util.UUID;

/**
 * Construcción terminada; su caja sigue contando para los choques con obras nuevas. {@code sourceHash}: hash del
 * esquemático con el que se hizo (null en las de antes de la v6: sin comprobar).
 */
public record BuildRecord(UUID id, UUID project, String structure, UUID owner, String world, int x, int y, int z,
                          Rotation rotation, Box box, long completedAt, String sourceHash) {

    public BuildRecord(UUID id, UUID project, String structure, UUID owner, String world, int x, int y, int z,
                       Rotation rotation, Box box, long completedAt) {
        this(id, project, structure, owner, world, x, y, z, rotation, box, completedAt, null);
    }
}
