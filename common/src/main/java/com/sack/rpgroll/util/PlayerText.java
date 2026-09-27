package com.sack.rpgroll.util;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Pattern;

/**
 * Texto que escribe un jugador y que luego ven otros: mensajes de chat,
 * susurros, /me, nombres y lemas de gremio o de equipo.
 * <p>
 * Los códigos de formato se quitan en vez de dejarse a la vista: "&amp;lhola"
 * se queda en "hola", ni en negrita ni con el "&amp;l" impreso. Así nadie
 * imita el formato del staff o del sistema, ni llena el chat de &amp;k. Los
 * {@code %placeholders%} no se tocan aquí: cada llamador tiene que resolver
 * PlaceholderAPI antes de meter el texto del jugador (ver MessageFormatter
 * de RPGRoll-Chat).
 */
public final class PlayerText {

    /** &amp;a, &amp;l, &amp;r, &amp;#RRGGBB y cada trozo de &amp;x&amp;R&amp;R&amp;G&amp;G&amp;B&amp;B (también con §). */
    private static final Pattern FORMAT_CODE = Pattern.compile("[&§](?:#[0-9a-fA-F]{6}|[0-9a-fk-orxA-FK-ORX])");

    /** Lo que ComponentUtils toma por una etiqueta MiniMessage, sin distinguir mayúsculas. */
    private static final Pattern MINI_TAG = Pattern.compile("(?i)</?(#[0-9a-f]{6}|[a-z_]+(:[^<>]*)?)>");

    private PlayerText() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Sin códigos &amp; ni §. En bucle: quitar el centro de "&amp;&amp;aa" deja otro "&amp;a". */
    public static String stripCodes(String text) {

        if (text == null || text.isEmpty()) {
            return text;
        }

        String current = text;
        String previous;

        do {
            previous = current;
            current = FORMAT_CODE.matcher(current).replaceAll("");
        } while (!current.equals(previous));

        return current;
    }

    /**
     * Para nombres, lemas y descripciones que acaban dentro de mensajes que
     * se parsean: ni códigos &amp; ni etiquetas MiniMessage (un
     * {@code <click:run_command:...>} en el nombre de un gremio se volvía
     * un enlace en los mensajes que lo nombran).
     */
    public static String clean(String text) {

        if (text == null) {
            return null;
        }

        String current = text;
        String previous;

        do {
            previous = current;
            current = MINI_TAG.matcher(stripCodes(current)).replaceAll("");
        } while (!current.equals(previous));

        return current.trim();
    }

    /** Para canales MiniMessage: las etiquetas del jugador se ven como texto, no se interpretan. */
    public static String escapeTags(String text) {
        return text == null ? null : MiniMessage.miniMessage().escapeTags(text);
    }

}
