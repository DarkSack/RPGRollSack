package com.sack.rpgroll.sackresourcepack.distribution.s3;

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
 * Sube el ZIP directo a un bucket S3 (o compatible) firmando el PUT con
 * {@link AwsSignatureV4} — a diferencia de {@code RemoteUploadService}
 * (PUT/POST genérico con un header de auth fijo), esto autentica de
 * verdad contra el algoritmo real de AWS.
 */
public class S3UploadService {

    private final Plugin plugin;
    private final AwsSignatureV4 signer = new AwsSignatureV4();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public S3UploadService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void upload(File zipFile, S3UploadSettings settings) {

        try {

            byte[] bytes = Files.readAllBytes(zipFile.toPath());

            String host = settings.pathStyle() ? settings.endpoint() : settings.bucket() + "." + settings.endpoint();
            String canonicalUri = settings.pathStyle() ? "/" + settings.bucket() + "/" + settings.objectKey()
                    : "/" + settings.objectKey();

            AwsSignatureV4.SignedRequest signed = signer.sign("PUT", host, canonicalUri, bytes, settings.accessKey(),
                    settings.secretKey(), settings.region(), "s3");

            String url = "https://" + host + canonicalUri;

            // OJO: NO se puede setear el header "Host" a mano vía HttpClient (es un header
            // restringido — java.net.http lo arma solo a partir de la URI). Por eso la URI
            // usa el mismo `host` que ya se firmó, para que ambos coincidan.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-amz-date", signed.amzDate())
                    .header("x-amz-content-sha256", signed.contentSha256Hex())
                    .header("Authorization", signed.authorizationHeader())
                    .header("Content-Type", "application/zip")
                    .timeout(Duration.ofSeconds(30))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            String message = success
                    ? "Subido a S3 correctamente (HTTP " + response.statusCode() + ")"
                    : "S3 respondió HTTP " + response.statusCode() + ": " + truncate(response.body());

            if (!success) {
                plugin.getLogger().warning("✘ Error subiendo a S3: " + message);
            }

            Bukkit.getPluginManager().callEvent(new PackUploadedEvent(success, message));

        } catch (Exception e) {

            plugin.getLogger().severe("✘ Error subiendo a S3: " + e.getMessage());
            Bukkit.getPluginManager().callEvent(new PackUploadedEvent(false, e.getMessage()));
        }
    }

    private String truncate(String raw) {
        return raw != null && raw.length() > 300 ? raw.substring(0, 300) + "..." : raw;
    }

}
