package com.sack.rpgroll.licensing;

import java.security.PublicKey;

/**
 * Una validación firmada por el servidor de licencias propio.
 * <p>
 * Es lo que se guarda en la caché del período de gracia en vez de un simple
 * {@code valid: true}: editar el archivo ya no sirve, porque el plugin vuelve a
 * comprobar la firma contra la clave actual antes de concederla, y la fecha que
 * cuenta es la que firmó el servidor, no la que alguien escriba.
 *
 * @param status   estado que firmó el servidor ({@code active})
 * @param server   id del servidor que hizo la petición
 * @param nonce    el aleatorio de aquella petición
 * @param issuedAt milisegundos, reloj del servidor de licencias
 */
record LicenseProof(String status, String server, String nonce, long issuedAt, String signature) {

    /** Si esta prueba es una validación positiva auténtica para esa clave y ese producto. */
    boolean verifies(PublicKey publicKey, String licenseKey, String resource) {
        String message = LicenseSignature.message(true, status, LicenseSignature.sha256Hex(licenseKey),
                resource, server, nonce, issuedAt);
        return LicenseSignature.verify(publicKey, message, signature);
    }
}
