package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;

import java.util.UUID;

public record WorkbenchRecord(UUID id, UUID owner, String world, int x, int y, int z, WorkbenchTier tier,
                              long serial, boolean orphan, long createdAt) {
}
