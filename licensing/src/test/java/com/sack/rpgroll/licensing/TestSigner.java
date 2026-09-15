package com.sack.rpgroll.licensing;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Hace de tienda en los tests: firma como lo hace {@code lib/firma-licencia.ts},
 * con un par de claves generado para la prueba.
 */
final class TestSigner {

    private final KeyPair pair;

    TestSigner() {
        try {
            pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    PublicKey publicKey() {
        return pair.getPublic();
    }

    String sign(String message) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(pair.getPrivate());
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Una validación positiva firmada, como la que guarda la caché. */
    LicenseProof proof(String licenseKey, String resource, String server, long issuedAt) {
        String nonce = "abc123";
        String message = LicenseSignature.message(true, "active", LicenseSignature.sha256Hex(licenseKey),
                resource, server, nonce, issuedAt);
        return new LicenseProof("active", server, nonce, issuedAt, sign(message));
    }

    /**
     * El cuerpo JSON que devolvería la tienda para esta petición.
     *
     * @param overrideLicense si no es null, se firma para OTRA clave (para probar el reenvío)
     * @param overrideNonce   si no es null, se firma con otro nonce (una respuesta vieja)
     */
    String responseFor(String requestBody, boolean valid, String status, String message,
                       String overrideLicense, String overrideNonce) {
        Map<String, String> form = parseForm(requestBody);
        long issuedAt = System.currentTimeMillis();

        String signed = LicenseSignature.message(valid, status,
                LicenseSignature.sha256Hex(overrideLicense != null ? overrideLicense : form.get("license")),
                form.get("resource"), form.get("server"),
                overrideNonce != null ? overrideNonce : form.get("nonce"), issuedAt);

        return "{\"valid\":" + valid + ",\"status\":\"" + status + "\""
                + (message != null ? ",\"message\":\"" + message + "\"" : "")
                + ",\"issued_at\":" + issuedAt + ",\"signature\":\"" + sign(signed) + "\"}";
    }

    String responseFor(String requestBody, boolean valid, String status) {
        return responseFor(requestBody, valid, status, null, null, null);
    }

    static Map<String, String> parseForm(String body) {
        Map<String, String> result = new HashMap<>();

        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                result.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }

        return result;
    }
}
