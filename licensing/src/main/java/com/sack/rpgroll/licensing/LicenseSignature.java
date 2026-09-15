package com.sack.rpgroll.licensing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Firma Ed25519 de las respuestas del servidor de licencias propio.
 * <p>
 * <b>Qué resuelve.</b> Sin firma, un {@code {"valid":true}} vale lo mismo venga
 * de la tienda o de cualquier servidor al que se redirija el dominio (un
 * {@code hosts}, un proxy con una CA propia instalada en el sistema). Y la caché
 * del período de gracia era un YAML que se editaba a mano. Con la firma, lo
 * único que el plugin acepta es algo que salió de quien tiene la clave privada,
 * que vive en Vercel y no en el jar.
 * <p>
 * <b>Qué se firma.</b> No el cuerpo JSON, sino un mensaje canónico que el plugin
 * reconstruye con sus propios datos: el hash de SU clave, SU producto, SU
 * servidor y el {@code nonce} que él mismo generó para esta petición. Una
 * respuesta legítima de otra clave, de otro servidor o de otro momento no
 * encaja y se descarta.
 * <p>
 * El formato tiene que coincidir byte a byte con {@code lib/firma-licencia.ts}
 * de la tienda.
 * <p>
 * <b>Qué no resuelve</b>: parchear el jar para saltarse esta comprobación, ni
 * atrasar el reloj del sistema para estirar el período de gracia. Nada del lado
 * del cliente lo impide del todo.
 */
final class LicenseSignature {

    static final String CONTEXT = "rpgroll-license-v1";

    private static final SecureRandom RANDOM = new SecureRandom();

    private LicenseSignature() {
    }

    /** Lo que se firma. Una línea por campo; los nulos van vacíos. */
    static String message(boolean valid, String status, String licenseHash, String resource,
                          String server, String nonce, long issuedAt) {
        return String.join("\n",
                CONTEXT,
                "valid=" + valid,
                "status=" + clean(status),
                "license=" + clean(licenseHash),
                "resource=" + clean(resource),
                "server=" + clean(server),
                "nonce=" + clean(nonce),
                "issued_at=" + issuedAt);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static boolean verify(PublicKey publicKey, String message, String signatureBase64) {
        if (publicKey == null || signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Clave pública X.509 (SPKI) en Base64, como la imprime {@code scripts/clave-firma.mjs}. */
    static PublicKey decodePublicKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }

        try {
            return KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64.trim())));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * La clave de licencia no viaja en claro dentro del mensaje firmado ni se
     * guarda en la caché: basta su hash para atar la firma a ella.
     */
    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(clean(value).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
