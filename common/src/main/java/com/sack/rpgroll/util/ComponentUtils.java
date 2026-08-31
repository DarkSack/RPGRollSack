package com.sack.rpgroll.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

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

    /** Un código legacy: &amp;a, &amp;l, &amp;#RRGGBB. */
    private static final Pattern LEGACY_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{6})");

    /**
     * Una etiqueta MiniMessage plausible: {@code <red>}, {@code </bold>},
     * {@code <gradient:#a:#b>}, {@code <#54daf4>}.
     * <p>
     * Deliberadamente NO matchea {@code <spawn|list|info>} ni {@code <jugador>}:
     * el nombre de una etiqueta real no lleva espacios ni barras verticales, y
     * se exige que sea una etiqueta conocida o un color hex.
     */
    private static final Pattern MINI_TAG = Pattern.compile(
            "</?(#[0-9a-fA-F]{6}|[a-z_]+(:[^<>]*)?)>");

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
     *
     * <p><b>Por qué el orden importa.</b> Antes bastaba con que el texto
     * tuviera un {@code <} y un {@code >} para mandarlo a MiniMessage. Como
     * los mensajes de uso llevan {@code <spawn|list|...>} y muchos otros
     * llevan placeholders tipo {@code <jugador>}, caían en MiniMessage — que
     * no entiende {@code &a} — y el código de color terminaba impreso literal
     * en el chat del comprador. Afectaba a 116 mensajes por idioma en 17
     * módulos.
     *
     * <p>Ahora manda el formato que realmente esté presente: si hay códigos
     * {@code &} se trata como legacy, y solo se usa MiniMessage ante una
     * etiqueta plausible.
     */
    public static Component parse(String text) {
        if (text == null || text.isBlank()) {
            return Component.empty();
        }

        // Legacy primero: es el formato de la enorme mayoría de los YAML, y
        // un texto con & casi nunca es MiniMessage.
        if (LEGACY_CODE.matcher(text).find()) {
            return LEGACY.deserialize(text);
        }

        if (MINI_TAG.matcher(text).find()) {
            return MINI_MESSAGE.deserialize(text);
        }

        // Sin marcas de formato: texto plano. Se pasa igual por LEGACY para
        // no perder un & suelto que el admin haya escrito a propósito.
        return LEGACY.deserialize(text);
    }

    /**
     * Igual que {@link #parse(String)}, pero aplica {@code fallback} cuando el
     * texto no trae ningún color propio.
     * <p>
     * Existe para que el color deje de estar fijo en el código Java: el
     * llamador pasa el color que usaba por defecto, y si el admin escribe uno
     * en el YAML, el del YAML gana. Sin esto, {@code Component.text(raw,
     * NamedTextColor.RED)} ignoraba cualquier color configurado.
     */
    public static Component parseWithDefault(String text, TextColor fallback) {

        Component parsed = parse(text);

        if (fallback == null || hasExplicitColor(text)) {
            return parsed;
        }

        return parsed.colorIfAbsent(fallback);
    }

    /** ¿El texto define su propio color (legacy o MiniMessage)? */
    private static boolean hasExplicitColor(String text) {
        return LEGACY_CODE.matcher(text).find() || MINI_TAG.matcher(text).find();
    }
}
