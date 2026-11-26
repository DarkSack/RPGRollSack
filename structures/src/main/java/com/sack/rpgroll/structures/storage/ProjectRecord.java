package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.structures.project.ProjectKind;
import com.sack.rpgroll.structures.project.ProjectState;

import java.util.UUID;

/**
 * {@code sourceHash}: hash del esquemático al crear la obra (null en obras de antes de la v3).
 * {@code moneyCents}: dinero cobrado al confirmar, en céntimos (0 en obras de antes de la v4).
 * {@code kind}: BUILD en las de antes de la v6; {@code buildId}: la construcción que repara o mejora (null en BUILD).
 * {@code repairBlocks}: bloques pagados de una REPAIR (0 en las demás); el motor no escribe más que esos.
 */
public record ProjectRecord(UUID id, UUID bench, String structure, UUID owner, ProjectState state, String world,
                            int x, int y, int z, Rotation rotation, int cursor, int placed, int skipped, int total,
                            String error, long createdAt, long updatedAt, String sourceHash, long moneyCents,
                            ProjectKind kind, UUID buildId, int repairBlocks) {

    public ProjectRecord(UUID id, UUID bench, String structure, UUID owner, ProjectState state, String world,
                         int x, int y, int z, Rotation rotation, int cursor, int placed, int skipped, int total,
                         String error, long createdAt, long updatedAt, String sourceHash, long moneyCents) {
        this(id, bench, structure, owner, state, world, x, y, z, rotation, cursor, placed, skipped, total, error,
                createdAt, updatedAt, sourceHash, moneyCents, ProjectKind.BUILD, null, 0);
    }

    public ProjectRecord(UUID id, UUID bench, String structure, UUID owner, ProjectState state, String world,
                         int x, int y, int z, Rotation rotation, int cursor, int placed, int skipped, int total,
                         String error, long createdAt, long updatedAt, String sourceHash) {
        this(id, bench, structure, owner, state, world, x, y, z, rotation, cursor, placed, skipped, total, error,
                createdAt, updatedAt, sourceHash, 0);
    }

    public ProjectRecord(UUID id, UUID bench, String structure, UUID owner, ProjectState state, String world,
                         int x, int y, int z, Rotation rotation, int cursor, int placed, int skipped, int total,
                         String error, long createdAt, long updatedAt) {
        this(id, bench, structure, owner, state, world, x, y, z, rotation, cursor, placed, skipped, total, error,
                createdAt, updatedAt, null);
    }
}
