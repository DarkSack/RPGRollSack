package com.sack.rpgroll.extras.backpack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * La textura de la cabeza se puede escribir de tres formas: el valor base64
 * (el "Value" de minecraft-heads.com), la URL de textures.minecraft.net o solo
 * su hash. Todo se convierte al base64 que espera el perfil.
 */
public final class BackpackTextures {

    private static final Pattern HASH = Pattern.compile("[0-9a-fA-F]{32,80}");
    private static final String URL_PREFIX = "http://textures.minecraft.net/texture/";

    private BackpackTextures() {
    }

    public static String toBase64(String texture) {

        if (texture == null || texture.isBlank()) {
            return "";
        }

        String value = texture.trim();
        String url = null;

        if (value.startsWith("http://") || value.startsWith("https://")) {
            url = value.replaceFirst("^https://", "http://");
        } else if (HASH.matcher(value).matches()) {
            url = URL_PREFIX + value;
        }

        if (url == null) {
            return value;
        }

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

}
