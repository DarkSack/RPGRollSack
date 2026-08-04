package com.sack.rpgroll.sackresourcepack.distribution;

import com.sack.rpgroll.sackresourcepack.event.PackUploadedEvent;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Sube el ZIP a un endpoint remoto genérico vía PUT/POST autenticado —
 * NO implementa firmas específicas de proveedor (ej. AWS SigV4 para
 * hablarle directo a un bucket S3): eso es un algoritmo de firma real,
 * sensible a errores sutiles, e imposible de verificar acá sin
 * credenciales reales. Sirve para un endpoint propio, un proxy, o
 * cualquier servicio que acepte un PUT/POST con un header de auth.
 */
public class RemoteUploadService {

    private final Plugin plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public RemoteUploadService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void upload(File zipFile, String url, String method, String authHeaderName, String authHeaderValue) {

        try {

            byte[] bytes = Files.readAllBytes(zipFile.toPath());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/zip")
                    .timeout(Duration.ofSeconds(30));

            if (authHeaderName != null && !authHeaderName.isBlank() && authHeaderValue != null
                    && !authHeaderValue.isBlank()) {
                requestBuilder.header(authHeaderName, authHeaderValue);
            }

            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(bytes);
            requestBuilder = "POST".equalsIgnoreCase(method) ? requestBuilder.POST(body) : requestBuilder.PUT(body);

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            String message = success
                    ? "Subido correctamente (HTTP " + response.statusCode() + ")"
                    : "El servidor remoto respondió HTTP " + response.statusCode();

            if (!success) {
                plugin.getLogger().warning("✘ Error subiendo el pack: " + message);
            }

            Bukkit.getPluginManager().callEvent(new PackUploadedEvent(success, message));

        } catch (Exception e) {

            plugin.getLogger().severe("✘ Error subiendo el pack a " + url + ": " + e.getMessage());
            Bukkit.getPluginManager().callEvent(new PackUploadedEvent(false, e.getMessage()));
        }
    }

}
