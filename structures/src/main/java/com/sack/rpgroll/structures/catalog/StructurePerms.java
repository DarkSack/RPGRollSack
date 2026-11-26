package com.sack.rpgroll.structures.catalog;

import org.bukkit.permissions.Permissible;

/** Permisos dinámicos por estructura o categoría ({@code rpgrollstructures.build.<id>}, {@code .category.<cat>}). */
public final class StructurePerms {

    private StructurePerms() {
    }

    /** Permitido salvo negado explícitamente: {@code build.*} sin children no concede el nodo de cada estructura. */
    public static boolean allowed(Permissible p, String node) {
        return !p.isPermissionSet(node) || p.hasPermission(node);
    }
}
