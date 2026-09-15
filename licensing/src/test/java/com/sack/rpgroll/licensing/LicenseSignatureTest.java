package com.sack.rpgroll.licensing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Compatibilidad con la tienda: estos vectores salen de {@code lib/firma-licencia.ts}.
 * Si alguno falla, los jars y /api/verify no se entienden y ninguna licencia
 * propia valida.
 */
class LicenseSignatureTest {

    @Test
    void canonicalMessageMatchesTheStoreByteForByte() {
        String message = LicenseSignature.message(true, "active",
                LicenseSignature.sha256Hex("RPGR-ABCDE-FGHIJ-KLMNO-PQRST"), "mobs", "srv-1", "00ff",
                1_800_000_000_000L);

        assertEquals(String.join("\n",
                "rpgroll-license-v1",
                "valid=true",
                "status=active",
                "license=16ab2793693f479a6d64b66b72c609b7b51a9bb207d4588dcb5435bcfd1b876c",
                "resource=mobs",
                "server=srv-1",
                "nonce=00ff",
                "issued_at=1800000000000"), message);
    }

    /** Firmado por la tienda con la clave privada real; se comprueba con la pública compilada. */
    @Test
    void aSignatureMadeByTheStoreVerifiesWithTheCompiledPublicKey() {
        String message = LicenseSignature.message(true, "active", LicenseSignature.sha256Hex("RPGR-X"),
                "core", "", "ab", 1_789_494_513_234L);
        String signature = "P86/RaNYI3/6MRKDsQi/Gt1+Trb+tsJO6ZZ0cuT2F77svZbpaw1OyeSVBkPO0Ei0quqOb89XJOhAv56D3hMlDw==";

        assertTrue(LicenseSignature.verify(LicenseSettings.signingPublicKey(), message, signature));
        assertFalse(LicenseSignature.verify(LicenseSettings.signingPublicKey(),
                message.replace("valid=true", "valid=false"), signature));
    }

    @Test
    void garbageSignaturesAreRejectedWithoutThrowing() {
        assertFalse(LicenseSignature.verify(LicenseSettings.signingPublicKey(), "x", "no-es-base64!!"));
        assertFalse(LicenseSignature.verify(LicenseSettings.signingPublicKey(), "x", ""));
        assertFalse(LicenseSignature.verify(null, "x", "AAAA"));
    }
}
