package com.sack.rpgroll.sackresourcepack.distribution.s3;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Implementación de AWS Signature Version 4 — sigue al pie de la letra el
 * algoritmo documentado por AWS (canonical request → string to sign →
 * clave de firma derivada por HMAC en cadena → firma) para autenticar un
 * PUT directo a un bucket S3 o cualquier endpoint S3-compatible (MinIO,
 * Cloudflare R2, Backblaze B2...), sin el SDK de AWS — solo
 * {@code javax.crypto}/{@code java.security}, ya en el JDK.
 * <p>
 * <b>No se pudo probar de punta a punta contra un bucket real en este
 * entorno</b> (no hay credenciales de AWS disponibles acá) — el algoritmo
 * en sí está implementado según la especificación oficial, pero antes de
 * confiar en esto en producción, probalo una vez contra tu bucket/
 * endpoint real.
 */
public class AwsSignatureV4 {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    public record SignedRequest(String authorizationHeader, String amzDate, String contentSha256Hex) {
    }

    public SignedRequest sign(String method, String host, String canonicalUri, byte[] payload, String accessKey,
            String secretKey, String region, String service) {

        Instant now = Instant.now();
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String payloadHash = sha256Hex(payload);

        // Headers firmados: solo host + los dos x-amz-* obligatorios — alcanza para
        // autenticar el PUT sin tener que firmar headers adicionales opcionales.
        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("host", host);
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("x-amz-date", amzDate);

        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeadersBuilder = new StringBuilder();

        for (var entry : headers.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
            signedHeadersBuilder.append(entry.getKey()).append(';');
        }

        String signedHeaders = signedHeadersBuilder.substring(0, signedHeadersBuilder.length() - 1);

        String canonicalRequest = method + '\n' + canonicalUri + '\n' + "" + '\n' + canonicalHeaders + '\n'
                + signedHeaders + '\n' + payloadHash;

        String credentialScope = dateStamp + '/' + region + '/' + service + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + '\n' + credentialScope + '\n'
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = deriveSigningKey(secretKey, dateStamp, region, service);
        String signatureHex = toHex(hmac(signingKey, stringToSign));

        String authorizationHeader = "AWS4-HMAC-SHA256 Credential=" + accessKey + '/' + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signatureHex;

        return new SignedRequest(authorizationHeader, amzDate, payloadHash);
    }

    private byte[] deriveSigningKey(String secretKey, String dateStamp, String region, String service) {
        byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, service);
        return hmac(kService, "aws4_request");
    }

    private byte[] hmac(byte[] key, String data) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular HMAC-SHA256", e);
        }
    }

    private String sha256Hex(byte[] data) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular SHA-256", e);
        }
    }

    private String toHex(byte[] bytes) {

        StringBuilder hex = new StringBuilder();

        for (byte b : bytes) {
            hex.append(String.format(Locale.ROOT, "%02x", b));
        }

        return hex.toString();
    }

}
