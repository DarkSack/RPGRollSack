package com.sack.rpgroll.chat.language;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;

/**
 * Idioma aprendible (Común/Élfico/Enano/Orco/Dracónico/...). Autoreado en
 * plugins/RPGRoll-Chat/languages/*.yml. {@code defaultForRaces} vincula el
 * idioma con razas de RPGRoll-Core por id — sin modificar el registro
 * {@code Race} de :api, para no acoplar Chat al modelo interno de razas.
 */
public record Language(String id, String displayName, char obfuscationChar, List<String> defaultForRaces)
        implements RPGContent {

    public Language {
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        defaultForRaces = defaultForRaces == null ? List.of() : List.copyOf(defaultForRaces);
    }

    /** Ofusca un mensaje para quien no conoce este idioma — spec: texto parcialmente ilegible. */
    public String obfuscate(String message) {

        StringBuilder builder = new StringBuilder(message.length());

        for (char c : message.toCharArray()) {
            builder.append(Character.isWhitespace(c) ? c : obfuscationChar);
        }

        return builder.toString();
    }

}
