package com.sack.rpgroll.chat.role;

import com.sack.rpgroll.common.content.RPGContent;

/** Rol de chat (spec: prefijo/sufijo/color/icono/prioridad/permisos) — autoreado en roles/*.yml. */
public record ChatRole(String id, String prefix, String suffix, String color, String icon, int priority,
        String permission) implements RPGContent {

    public ChatRole {
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        color = color == null || color.isBlank() ? "WHITE" : color;
        icon = icon == null ? "" : icon;
    }

    public boolean matches(org.bukkit.entity.Player player) {
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

}
