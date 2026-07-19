package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static Component top() {
        return Component.text("╔════════════════════════════════╗", NamedTextColor.GOLD);
    }

    public static Component separator() {
        return Component.text("╠════════════════════════════════╣", NamedTextColor.GOLD);
    }

    public static Component bottom() {
        return Component.text("╚════════════════════════════════╝", NamedTextColor.GOLD);
    }

    public static Component title(String title) {
        return Component.text("╠ ", NamedTextColor.YELLOW)
                .append(
                        Component.text(title,
                                NamedTextColor.GOLD,
                                TextDecoration.BOLD))
                .append(Component.text(" ╣", NamedTextColor.YELLOW));
    }

    public static Component section(String title) {
        return Component.text("╠ ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(title, NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD));
    }

    public static Component line(NamedTextColor labelColor,
            String label,
            Object value) {

        return Component.text("╠ ", labelColor)
                .append(Component.text(label + ": ", labelColor))
                .append(Component.text(String.valueOf(value), NamedTextColor.WHITE));
    }

    public static Component blank() {
        return Component.empty();
    }
}