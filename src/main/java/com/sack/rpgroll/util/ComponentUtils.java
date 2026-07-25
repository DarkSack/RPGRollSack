package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ComponentUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private ComponentUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte un String en un Component.
     *
     * Soporta:
     * - MiniMessage (<gold>, <red>, etc.)
     * - Legacy (&6, &c, &l, etc.)
     */
    public static Component parse(String text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        // Si contiene etiquetas MiniMessage asumimos ese formato
        if (text.indexOf('<') >= 0 && text.indexOf('>') >= 0) {
            return MINI_MESSAGE.deserialize(text);
        }

        // Si no, interpretamos códigos &
        return LEGACY.deserialize(text);
    }
}