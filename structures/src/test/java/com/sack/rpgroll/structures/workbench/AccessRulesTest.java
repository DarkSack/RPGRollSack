package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccessRulesTest {
    private final UUID owner = UUID.randomUUID(), friend = UUID.randomUUID(), stranger = UUID.randomUUID();
    private final WorkbenchRecord bench = new WorkbenchRecord(UUID.randomUUID(), owner, "w", 0, 0, 0,
            WorkbenchTier.CONSTRUCTOR, 1, false, 0);
    private final Set<UUID> trusted = Set.of(friend);

    @Test void ownerTrustedAndBypassCanUseStrangerCannot() {
        assertTrue(AccessRules.canUse(owner, bench, trusted, false));
        assertTrue(AccessRules.canUse(friend, bench, trusted, false));
        assertTrue(AccessRules.canUse(stranger, bench, trusted, true));
        assertFalse(AccessRules.canUse(stranger, bench, trusted, false));
    }

    @Test void onlyOwnerOrBypassCanManage() {
        assertTrue(AccessRules.canManage(owner, bench, trusted, false));
        assertTrue(AccessRules.canManage(stranger, bench, trusted, true));
        assertFalse(AccessRules.canManage(friend, bench, trusted, false));
        assertFalse(AccessRules.canManage(stranger, bench, trusted, false));
    }
}
