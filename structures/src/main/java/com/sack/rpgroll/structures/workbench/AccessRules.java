package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.storage.WorkbenchRecord;

import java.util.Set;
import java.util.UUID;

/** Quién puede usar y quién puede gestionar una mesa. Lógica pura. */
public final class AccessRules {
    private AccessRules() {}

    /** Dueño, de confianza o bypass. */
    public static boolean canUse(UUID player, WorkbenchRecord bench, Set<UUID> trusted, boolean bypass) {
        return canManage(player, bench, trusted, bypass) || trusted.contains(player);
    }

    /** Solo dueño o bypass: mejorar, dar acceso, romper. Ignora {@code trusted}: los de confianza no gestionan. */
    public static boolean canManage(UUID player, WorkbenchRecord bench, Set<UUID> trusted, boolean bypass) {
        return bypass || bench.owner().equals(player);
    }
}
