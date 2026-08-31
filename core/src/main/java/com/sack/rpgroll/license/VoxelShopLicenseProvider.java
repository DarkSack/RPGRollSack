package com.sack.rpgroll.license;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifica la compra contra la API de voxel.shop (antes Polymart — es el
 * mismo marketplace rebrandeado: {@code polymart.org/wiki/api} redirige a
 * {@code voxel.shop/wiki/api} y la API sigue publicada bajo el host
 * histórico {@code api.polymart.org}, por eso el endpoint no cambia).
 * <p>
 * La clave de licencia llega vía el placeholder {@code %%__LICENSE__%%},
 * que el marketplace sustituye por la clave real del comprador al momento
 * de la descarga — nunca vive en el repositorio.
 * <p>
 * Forma de la respuesta (verificada contra la API real):
 *
 * <pre>{@code
 * {
 *   "request":  { "action": "...", "time": 1788126995 },
 *   "response": {
 *     "success": true,
 *     "resource": { "id": "4", "purchaseValid": true,
 *                   "purchaseStatus": "Confirmed", "purchaseTime": 1780804532 },
 *     "user":     { "id": 418054 }
 *   }
 * }
 * }</pre>
 *
 * En un fallo, {@code errors} viene DENTRO de {@code response} (no en la
 * raíz) y {@code global} es un string, no un array — por ejemplo
 * {@code {"success":false,"error":"BAD_PRODUCT_ID","errors":{"global":"BAD_PRODUCT_ID"}}}.
 */
public class VoxelShopLicenseProvider implements LicenseProvider {

    private static final String ENDPOINT = "https://api.polymart.org/v1/verifyPurchase";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final String endpoint;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public VoxelShopLicenseProvider() {
        this(ENDPOINT);
    }

    /** Constructor para tests (endpoint apuntando a un servidor local). */
    public VoxelShopLicenseProvider(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String name() {
        return "voxel.shop";
    }

    @Override
    public LicenseResult validate(String licenseKey, String resourceId) {

        try {
            String body = "license=" + urlEncode(licenseKey) + "&resource_id=" + urlEncode(resourceId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "RPGRoll-License-Check/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return parse(send(request).body());

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return LicenseResult.unknown("No se pudo contactar a voxel.shop: " + describe(e));
        }
    }

    /** Mismo reintento que en el canal propio — ver {@link LicenseHttp}. */
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IOException e) {

            if (!LicenseHttp.isTrustFailure(e)) {
                throw e;
            }

            HttpClient fallback = LicenseHttp.platformTrustClient().orElseThrow(() -> e);

            return fallback.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private String describe(Exception e) {

        if (e instanceof IOException io && LicenseHttp.isTrustFailure(io)) {
            return "el Java de este servidor no pudo validar el certificado de voxel.shop."
                    + " Suele ser un cacerts vacío o desactualizado en la instalación de Java,"
                    + " o un antivirus interceptando HTTPS. Probá actualizar Java o arrancar con"
                    + " -Djavax.net.ssl.trustStoreType=Windows-ROOT (Windows).";
        }

        return e.getMessage();
    }

    /**
     * Cualquier forma inesperada se trata como UNKNOWN, nunca como una
     * licencia inválida: un cambio en la API del marketplace no debe
     * bloquear a un comprador legítimo. Por eso todo el parseo va dentro de
     * un catch de RuntimeException — {@code validate} solo captura
     * IOException, así que una excepción de Gson acá tumbaría el onEnable.
     */
    private LicenseResult parse(String rawBody) {

        try {
            JsonObject root = JsonParser.parseString(rawBody).getAsJsonObject();

            JsonObject response = root.has("response") && root.get("response").isJsonObject()
                    ? root.getAsJsonObject("response")
                    : null;

            if (response == null || !response.has("success") || !response.get("success").getAsBoolean()) {
                return LicenseResult.unknown("voxel.shop rechazó la consulta: " + extractFirstError(response, root));
            }

            JsonObject resource = response.has("resource") && response.get("resource").isJsonObject()
                    ? response.getAsJsonObject("resource")
                    : null;

            if (resource == null || !resource.has("purchaseValid")) {
                return LicenseResult.unknown("Respuesta de voxel.shop sin datos de compra.");
            }

            boolean purchaseValid = resource.get("purchaseValid").getAsBoolean();
            String status = resource.has("purchaseStatus") ? resource.get("purchaseStatus").getAsString() : "?";

            return purchaseValid
                    ? LicenseResult.valid("Compra verificada (" + status + ").")
                    : LicenseResult.invalid("Compra inválida en voxel.shop (estado: " + status + ").");

        } catch (RuntimeException e) {
            return LicenseResult.unknown("Respuesta inesperada de voxel.shop: " + e.getMessage());
        }
    }

    /** Busca el detalle del error primero en {@code response}, después en la raíz. */
    private String extractFirstError(JsonObject response, JsonObject root) {

        for (JsonObject candidate : new JsonObject[] {response, root}) {

            if (candidate == null || !candidate.has("errors") || !candidate.get("errors").isJsonObject()) {
                continue;
            }

            String detail = readGlobal(candidate.getAsJsonObject("errors"));

            if (detail != null) {
                return detail;
            }
        }

        return "sin detalle";
    }

    /** {@code global} puede venir como string suelto o como array de strings. */
    private String readGlobal(JsonObject errors) {

        if (!errors.has("global")) {
            return null;
        }

        JsonElement global = errors.get("global");

        if (global.isJsonArray()) {
            return global.getAsJsonArray().isEmpty() ? null : global.getAsJsonArray().get(0).getAsString();
        }

        return global.isJsonPrimitive() ? global.getAsString() : null;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

}
