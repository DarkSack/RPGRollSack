package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ComponentUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * {@code .hexColors()} habilita tanto {@code &#RRGGBB} (hex "plano") como
     * el formato repetido de BungeeCord {@code &x&R&R&G&G&B&B} — este último
     * siempre se puede DEserializar con solo ese flag, aunque no se use
     * {@code .useUnusualXRepeatedCharacterHexFormat()} (ese método solo
     * afecta cómo se vuelve a SERIALIZAR un Component a texto, no el parseo).
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private ComponentUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte un String en un Component.
     *
     * Soporta:
     * - MiniMessage (&lt;gold&gt;, &lt;red&gt;, &lt;gradient:#54daf4:#545eb6&gt;...&lt;/gradient&gt;, etc.)
     * - Legacy clásico (&amp;6, &amp;c, &amp;l, etc.)
     * - Legacy hex (&amp;#54DAF4) y el formato BungeeCord (&amp;x&amp;5&amp;4&amp;D&amp;A&amp;F&amp;4)
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