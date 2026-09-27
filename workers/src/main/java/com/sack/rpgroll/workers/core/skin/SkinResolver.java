package com.sack.rpgroll.workers.core.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte lo que escribe el staff en una skin firmada:
 * <ul>
 *   <li>un enlace de MineSkin ({@code mineskin.org/...}), {@code mineskin:<id>} o el uuid de una
 *   skin de MineSkin: se pide a su API pública, como hace RPGRoll-NPCs;</li>
 *   <li>el nombre de un jugador de Minecraft: se copia la skin que lleva hoy (si luego se la
 *   cambia, el worker se queda con esta).</li>
 * </ul>
 * Las consultas salen del hilo principal y las respuestas vuelven a él. Los errores llegan como
 * la clave de idioma del motivo ({@code skin.bad_input}, {@code skin.not_found},
 * {@code skin.unreachable}).
 */
public class SkinResolver {

    private static final String MINESKIN_API = "https://api.mineskin.org/v2/skins/";
    private static final Pattern MINESKIN_URL = Pattern.compile("mineskin\\.org/(?:skins/)?([A-Za-z0-9-]+)");
    private static final Pattern MINESKIN_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final Pattern SKIN_UUID = Pattern.compile(
            "[0-9a-fA-F]{32}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final Plugin plugin;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public SkinResolver(Plugin plugin) {
        this.plugin = plugin;
    }

    public void resolve(String input, Consumer<SkinTexture> onSuccess, Consumer<String> onError) {

        String text = input.trim();
        Matcher url = MINESKIN_URL.matcher(text);

        if (url.find()) {
            fromMineSkin(url.group(1), onSuccess, onError);
        } else if (text.toLowerCase(Locale.ROOT).startsWith("mineskin:")) {
            fromMineSkin(text.substring("mineskin:".length()).trim(), onSuccess, onError);
        } else if (SKIN_UUID.matcher(text).matches()) {
            fromMineSkin(text, onSuccess, onError);
        } else if (PLAYER_NAME.matcher(text).matches()) {
            fromPlayer(text, onSuccess, onError);
        } else {
            onError.accept("skin.bad_input");
        }
    }

    private void fromMineSkin(String id, Consumer<SkinTexture> onSuccess, Consumer<String> onError) {

        if (!MINESKIN_ID.matcher(id).matches()) {
            onError.accept("skin.bad_input");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(MINESKIN_API + id))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "RPGRoll-Workers/1.0")
                .GET()
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {

            if (error != null) {
                plugin.getLogger().warning("✘ No se pudo consultar MineSkin: " + error.getMessage());
                finish(null, "skin.unreachable", onSuccess, onError);
                return;
            }

            SkinTexture skin = response.statusCode() == 200 ? parseMineSkin(response.body()) : null;
            finish(skin, "skin.not_found", onSuccess, onError);
        });
    }

    private void fromPlayer(String name, Consumer<SkinTexture> onSuccess, Consumer<String> onError) {

        PlayerProfile profile = Bukkit.createProfile(name);

        profile.update().whenComplete((filled, error) -> {

            if (error != null) {
                plugin.getLogger().warning("✘ No se pudo buscar el perfil de '" + name + "': " + error.getMessage());
                finish(null, "skin.unreachable", onSuccess, onError);
                return;
            }

            SkinTexture skin = null;

            for (ProfileProperty property : filled.getProperties()) {
                if ("textures".equals(property.getName())) {
                    skin = new SkinTexture(property.getValue(), property.getSignature());
                }
            }

            finish(skin, "skin.not_found", onSuccess, onError);
        });
    }

    private void finish(SkinTexture skin, String reason, Consumer<SkinTexture> onSuccess, Consumer<String> onError) {

        if (!plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (skin != null) {
                onSuccess.accept(skin);
            } else {
                onError.accept(reason);
            }
        });
    }

    /** {@code {"skin": {"texture": {"data": {"value": ..., "signature": ...}}}}}, o null si no es eso. */
    private static SkinTexture parseMineSkin(String body) {

        try {
            JsonObject data = JsonParser.parseString(body).getAsJsonObject()
                    .getAsJsonObject("skin").getAsJsonObject("texture").getAsJsonObject("data");
            JsonElement value = data.get("value");
            JsonElement signature = data.get("signature");

            if (value == null || value.isJsonNull() || value.getAsString().isBlank()) {
                return null;
            }

            return new SkinTexture(value.getAsString(),
                    signature == null || signature.isJsonNull() ? null : signature.getAsString());
        } catch (RuntimeException e) {
            return null;
        }
    }

}
