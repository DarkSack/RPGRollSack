package com.sack.rpgroll.guilds.guild;

import com.sack.rpgroll.util.PlayerText;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * El nombre del gremio con colores, para diferenciarlo en la TAB y en los
 * menús: {@code &x&5&4&D&A&F&4P&x&5&4&9&C&D&5a…} (degradados por letra),
 * {@code &#RRGGBB} y los colores clásicos {@code &0}–{@code &f}.
 * <p>
 * Solo colores: negrita, {@code &k}, {@code &r} y demás formatos se quitan,
 * y el texto que queda tiene que ser exactamente el nombre del gremio. Así
 * nadie se pone un nombre distinto del suyo ni imita al staff, y el nombre
 * normal ({@link PlayerText#clean}) sigue siendo el que vale para todo lo demás.
 */
public final class GuildNameStyle {

    /** Lo que se guarda tras normalizar; un nombre de 24 letras con un color por letra son 216. */
    public static final int MAX_LENGTH = 400;

    /** Un código, en el orden en que se prueba: &x repetido, &#RRGGBB y uno de una letra. */
    private static final Pattern CODE = Pattern.compile(
            "&x((?:&[0-9a-f]){6})|&#([0-9a-f]{6})|&([0-9a-fk-orx])", Pattern.CASE_INSENSITIVE);

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private GuildNameStyle() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * El estilo normalizado ({@code &x…} pasa a {@code &#RRGGBB}, más corto) o
     * vacío si no es el nombre {@code plainName} con solo códigos de color.
     */
    public static Optional<String> parse(String input, String plainName) {

        if (input == null || input.isBlank() || plainName == null || input.length() > MAX_LENGTH * 2) {
            return Optional.empty();
        }

        String source = input.replace('§', '&');
        Matcher matcher = CODE.matcher(source);
        StringBuilder out = new StringBuilder();
        StringBuilder visible = new StringBuilder();
        int last = 0;

        while (matcher.find()) {
            String text = source.substring(last, matcher.start());
            out.append(text);
            visible.append(text);
            last = matcher.end();

            if (matcher.group(1) != null) {
                out.append("&#").append(matcher.group(1).replace("&", "").toUpperCase(Locale.ROOT));
            } else if (matcher.group(2) != null) {
                out.append("&#").append(matcher.group(2).toUpperCase(Locale.ROOT));
            } else {
                char code = Character.toLowerCase(matcher.group(3).charAt(0));
                // Un &x suelto es un hex roto: no se adivina qué color quería.
                if (code == 'x') {
                    return Optional.empty();
                }
                // Solo colores; negrita, &k, &r y demás formatos se quitan.
                if ("0123456789abcdef".indexOf(code) >= 0) {
                    out.append('&').append(code);
                }
            }
        }

        String tail = source.substring(last);
        out.append(tail);
        visible.append(tail);

        // Lo que se ve tiene que ser el nombre tal cual: sin etiquetas, códigos de más ni texto extra.
        if (!visible.toString().equals(plainName) || !PlayerText.clean(plainName).equals(plainName)
                || out.length() > MAX_LENGTH) {
            return Optional.empty();
        }

        return Optional.of(out.toString());
    }

    /** Un color ya normalizado: &amp;#RRGGBB o &amp;0-&amp;f. */
    private static final Pattern STORED_CODE = Pattern.compile("&#[0-9A-F]{6}|&[0-9a-f]");

    /**
     * El estilo con cada color corrido {@code step} letras a la derecha (con
     * vuelta al principio): llamado con un paso que avanza, los colores
     * recorren el nombre como una ola. Un solo color, o ninguno, no se mueve.
     */
    public static String animate(String styled, long step) {

        if (styled == null) {
            return null;
        }

        java.util.List<String> colors = new java.util.ArrayList<>();
        StringBuilder letters = new StringBuilder();
        String current = "";
        Matcher matcher = STORED_CODE.matcher(styled);
        int last = 0;

        while (matcher.find()) {
            addLetters(styled.substring(last, matcher.start()), current, colors, letters);
            current = matcher.group();
            last = matcher.end();
        }
        addLetters(styled.substring(last), current, colors, letters);

        if (colors.stream().distinct().count() < 2) {
            return styled;
        }

        // Lo que no tenía color se ve blanco: al moverse no puede heredar el de la letra anterior.
        colors.replaceAll(color -> color.isEmpty() ? "&f" : color);

        int n = colors.size();
        int shift = (int) Math.floorMod(step, (long) n);
        StringBuilder out = new StringBuilder();
        String previous = null;

        for (int i = 0; i < n; i++) {
            String color = colors.get(Math.floorMod(i - shift, n));
            if (!color.equals(previous)) {
                out.append(color);
                previous = color;
            }
            out.append(letters.charAt(i));
        }

        return out.toString();
    }

    private static void addLetters(String text, String color, java.util.List<String> colors, StringBuilder letters) {
        for (int i = 0; i < text.length(); i++) {
            colors.add(color);
            letters.append(text.charAt(i));
        }
    }

    /** Como {@link #display}, con los colores del estilo propio movidos {@code step} letras. */
    public static String displayAnimated(Guild guild, long step) {
        return guild.styledName() != null ? animate(guild.styledName(), step) : display(guild);
    }

    /** El nombre con el color de la paleta, para el gremio que no tiene estilo propio. */
    public static String fallback(String name, NamedTextColor color) {
        return LEGACY.serialize(Component.text(name, color));
    }

    /** Lo que se enseña: el estilo propio si lo hay, si no el nombre con su color. */
    public static String display(Guild guild) {
        return guild.styledName() != null ? guild.styledName() : fallback(guild.name(), guild.color());
    }

}
