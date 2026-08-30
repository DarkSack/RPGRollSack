package com.sack.rpgroll.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifica la compra contra un servidor de licencias propio — el canal de
 * ventas directas (Ko-fi, Patreon, transferencia, etc.), donde no hay un
 * marketplace que sustituya {@code %%__LICENSE__%%} ni exponga una API de
 * verificación.
 * <p>
 * El endpoint lo configura el vendedor en {@code license.yml}
 * ({@code endpoint:}), así que el plugin no tiene ninguna URL propia
 * hardcodeada.
 * <p>
 * <b>Contrato esperado.</b> {@code POST <endpoint>} con
 * {@code application/x-www-form-urlencoded} y los campos {@code license} y
 * {@code resource}. La respuesta debe ser JSON:
 *
 * <pre>{@code
 * { "valid": true,  "status": "active",  "message": "opcional" }
 * { "valid": false, "status": "revoked", "message": "opcional" }
 * }</pre>
 *
 * Mapeo deliberado sobre los tres estados de {@link LicenseResult}:
 * <ul>
 *   <li>{@code valid: true} → VALID.</li>
 *   <li>{@code valid: false} → INVALID: clave desconocida o REVOCADA. Bloquea
 *       de inmediato, sin período de gracia.</li>
 *   <li>caída de red, HTTP != 2xx o cuerpo ilegible → UNKNOWN, que activa el
 *       período de gracia de {@link LicenseCache}. Es intencional: si el
 *       servidor propio se cae, un comprador legítimo NO queda bloqueado.</li>
 * </ul>
 *
 * Consecuencia a tener presente: como una caída se trata con gracia, una
 * licencia revocada sigue funcionando mientras dure esa ventana si el
 * servidor está inalcanzable. Es el costo inevitable de tolerar cortes; la
 * alternativa (bloquear ante cualquier fallo de red) castiga a los
 * compradores legítimos por un problema del vendedor.
 */
public class SelfHostedLicenseProvider implements LicenseProvider {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final String endpoint;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public SelfHostedLicenseProvider(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String name() {
        return "servidor propio";
    }

    @Override
    public LicenseResult validate(String licenseKey, String resourceId) {

        if (endpoint == null || endpoint.isBlank()) {
            return LicenseResult.invalid(
                    "El modo 'self-hosted' necesita un 'endpoint' en license.yml y no hay ninguno configurado.");
        }

        try {
            String body = "license=" + urlEncode(licenseKey) + "&resource=" + urlEncode(resourceId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "RPGRoll-License-Check/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                return LicenseResult.unknown(
                        "El servidor de licencias respondió HTTP " + response.statusCode() + ".");
            }

            return parse(response.body());

        } catch (IllegalArgumentException e) {
            return LicenseResult.invalid("El 'endpoint' de license.yml no es una URL válida: " + e.getMessage());

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return LicenseResult.unknown("No se pudo contactar al servidor de licencias: " + e.getMessage());
        }
    }

    /** Igual que en voxel.shop: una respuesta rara es UNKNOWN, nunca INVALID. */
    private LicenseResult parse(String rawBody) {

        try {
            JsonObject root = JsonParser.parseString(rawBody).getAsJsonObject();

            if (!root.has("valid")) {
                return LicenseResult.unknown("El servidor de licencias no devolvió el campo 'valid'.");
            }

            String status = root.has("status") ? root.get("status").getAsString() : "?";
            String message = root.has("message") && root.get("message").isJsonPrimitive()
                    ? root.get("message").getAsString()
                    : null;

            if (root.get("valid").getAsBoolean()) {
                return LicenseResult.valid(message != null ? message : "Compra verificada (" + status + ").");
            }

            return LicenseResult.invalid(
                    message != null ? message : "Licencia rechazada por el servidor (estado: " + status + ").");

        } catch (RuntimeException e) {
            return LicenseResult.unknown("Respuesta inesperada del servidor de licencias: " + e.getMessage());
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

}
