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
 * Verifica la compra contra la API pública de Polymart
 * ({@code POST https://api.polymart.org/v1/verifyPurchase}), pasando la
 * clave de licencia (sustituida por Polymart al momento de la descarga vía
 * el placeholder {@code %%__LICENSE__%%}) y el id público del recurso.
 * <p>
 * Forma de la respuesta esperada (confirmada contra la documentación
 * pública de Polymart):
 *
 * <pre>{@code
 * {
 *   "response": {
 *     "success": true,
 *     "resource": {
 *       "id": "123",
 *       "purchaseValid": true,
 *       "purchaseStatus": "Confirmed"
 *     }
 *   },
 *   "errors": { "global": [ "..." ] }
 * }
 * }</pre>
 */
public class PolymartLicenseProvider implements LicenseProvider {

    private static final String ENDPOINT = "https://api.polymart.org/v1/verifyPurchase";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @Override
    public String name() {
        return "Polymart";
    }

    @Override
    public LicenseResult validate(String licenseKey, String resourceId) {

        try {
            String body = "license=" + urlEncode(licenseKey) + "&resource_id=" + urlEncode(resourceId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "RPGRoll-License-Check/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return parse(response.body());

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return LicenseResult.unknown("No se pudo contactar a Polymart: " + e.getMessage());
        }
    }

    private LicenseResult parse(String rawBody) {

        JsonObject root;

        try {
            root = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (Exception e) {
            return LicenseResult.unknown("Respuesta inesperada de Polymart (no es JSON válido).");
        }

        JsonObject response = root.has("response") ? root.getAsJsonObject("response") : null;

        if (response == null || !response.has("success") || !response.get("success").getAsBoolean()) {
            String error = extractFirstError(root);
            return LicenseResult.unknown("Polymart rechazó la consulta: " + error);
        }

        JsonObject resource = response.has("resource") ? response.getAsJsonObject("resource") : null;

        if (resource == null || !resource.has("purchaseValid")) {
            return LicenseResult.unknown("Respuesta de Polymart sin datos de compra.");
        }

        boolean purchaseValid = resource.get("purchaseValid").getAsBoolean();
        String status = resource.has("purchaseStatus") ? resource.get("purchaseStatus").getAsString() : "?";

        return purchaseValid
                ? LicenseResult.valid("Compra verificada (" + status + ").")
                : LicenseResult.invalid("Compra inválida en Polymart (estado: " + status + ").");
    }

    private String extractFirstError(JsonObject root) {

        if (!root.has("errors")) {
            return "sin detalle";
        }

        JsonObject errors = root.getAsJsonObject("errors");

        if (errors.has("global") && errors.getAsJsonArray("global").size() > 0) {
            return errors.getAsJsonArray("global").get(0).getAsString();
        }

        return "sin detalle";
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

}
