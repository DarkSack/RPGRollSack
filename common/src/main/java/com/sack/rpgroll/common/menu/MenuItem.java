package com.sack.rpgroll.common.menu;

import java.util.List;
import java.util.Objects;

/**
 * Un ítem clickeable dentro de un {@link MenuDefinition}. Al hacer click,
 * ejecuta su lista de {@link MenuAction}. Con {@code permission}, solo lo ve
 * quien tenga ese permiso.
 */
public record MenuItem(
        int slot,
        String material,
        String displayName,
        List<String> lore,
        List<MenuAction> actions,
        String permission) {

    public MenuItem {
        Objects.requireNonNull(material, "material no puede ser null");
        displayName = displayName == null ? "" : displayName;
        lore = lore == null ? List.of() : List.copyOf(lore);
        actions = actions == null ? List.of() : List.copyOf(actions);
        permission = permission == null || permission.isBlank() ? null : permission.trim();
    }

    public MenuItem(int slot, String material, String displayName, List<String> lore, List<MenuAction> actions) {
        this(slot, material, displayName, lore, actions, null);
    }

}
