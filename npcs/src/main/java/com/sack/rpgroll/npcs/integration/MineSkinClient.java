package com.sack.rpgroll.npcs.integration;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente para la API pública de MineSkin (https://mineskin.org).
 * Resuelve un uuid/shortId/URL de skin a su value+signature reales.
 */
public class MineSkinClient {

    private static final String API_BASE = "https://api.mineskin.org/v2/skins/";

    // Extrae el identificador tanto de una URL completa como de un id suelto
    private static final Pattern ID_PATTERN = Pattern.compile("mineskin\\.org/([a-zA-Z0-9]+)|^([a-zA-Z0-9]+)$");

    private final Plugin plugin;
    private final Gson gson = new Gson();
    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    public MineSkinClient(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Resuelve un skin por su uuid o shortId (aceptando también la URL
     * completa de mineskin.org, de la cual se extrae el identificador).
     * El callback (value, signature) se ejecuta en el hilo principal del
     * servidor — seguro de usar para tocar la sesión de edición o la GUI.
     *
     * @param onError se ejecuta en el hilo principal si la resolución falla
     */
    public void resolve(String input, BiConsumer<String, String> onSuccess, Runnable onError) {

        String id = extractId(input);

        Request request = new Request.Builder()
                .url(API_BASE + id)
                .header("Accept", "application/json")
                .header("User-Agent", "RPGRoll-NPCs/1.0")
                .get()
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                plugin.getLogger().warning("✘ Error consultando MineSkin: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, onError);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {

                try (response) {

                    if (!response.isSuccessful() || response.body() == null) {
                        Bukkit.getScheduler().runTask(plugin, onError);
                        return;
                    }

                    String json = response.body().string();
                    MineSkinResponse parsed = gson.fromJson(json, MineSkinResponse.class);

                    if (parsed == null || !parsed.success || parsed.skin == null
                            || parsed.skin.texture == null || parsed.skin.texture.data == null) {
                        Bukkit.getScheduler().runTask(plugin, onError);
                        return;
                    }

                    String value = parsed.skin.texture.data.value;
                    String signature = parsed.skin.texture.data.signature;

                    Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(value, signature));

                } catch (Exception e) {
                    plugin.getLogger().warning("✘ Error parseando respuesta de MineSkin: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, onError);
                }
            }
        });
    }

    private String extractId(String input) {

        Matcher matcher = ID_PATTERN.matcher(input.trim());

        if (matcher.find()) {

            if (matcher.group(1) != null) {
                return matcher.group(1);
            }

            return matcher.group(2);
        }

        return input.trim();
    }

}