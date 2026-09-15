package com.sack.rpgroll.licensing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfHostedLicenseProviderTest {

    private final TestSigner store = new TestSigner();

    private SelfHostedLicenseProvider provider(String endpoint) {
        return new SelfHostedLicenseProvider(endpoint, null, null, store.publicKey());
    }

    private LicenseResult validateAgainst(int statusCode, UnaryOperator<String> body) throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.respondingWith(statusCode, body)) {
            return provider(fixture.endpoint()).validate("KEY", "rpgroll");
        }
    }

    private LicenseResult validateAgainst(int statusCode, String body) throws Exception {
        return validateAgainst(statusCode, request -> body);
    }

    @Test
    void signedActiveLicenseIsValidAndCarriesItsProof() throws Exception {
        LicenseResult result = validateAgainst(200, request -> store.responseFor(request, true, "active"));

        assertEquals(LicenseResult.Status.VALID, result.status());
        assertNotNull(result.proof());
        assertTrue(result.proof().verifies(store.publicKey(), "KEY", "rpgroll"));
    }

    // El punto del canal propio: revocar una clave la bloquea de inmediato,
    // sin período de gracia (a diferencia de una caída del servidor).
    @Test
    void signedRevokedLicenseIsInvalidNotUnknown() throws Exception {
        LicenseResult result = validateAgainst(200, request -> store.responseFor(request, false, "revoked"));

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("revoked"));
        assertNull(result.proof());
    }

    @Test
    void serverMessageIsPreferredOverTheGenericOne() throws Exception {
        LicenseResult result = validateAgainst(200,
                request -> store.responseFor(request, false, "revoked", "Reembolsada el 2026-08-01", null, null));

        assertEquals("Reembolsada el 2026-08-01", result.message());
    }

    // ── Lo que la firma impide ──────────────────────────────────────────────

    @Test
    void unsignedValidResponseFromAFakeServerGrantsNothing() throws Exception {
        LicenseResult result = validateAgainst(200, "{\"valid\":true,\"status\":\"active\"}");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void responseSignedWithAnotherKeyGrantsNothing() throws Exception {
        TestSigner impostor = new TestSigner();

        LicenseResult result = validateAgainst(200, request -> impostor.responseFor(request, true, "active"));

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void genuineResponseForAnotherLicenseCannotBeReused() throws Exception {
        LicenseResult result = validateAgainst(200,
                request -> store.responseFor(request, true, "active", null, "SOMEONE-ELSES-KEY", null));

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void genuineResponseToAnOlderRequestCannotBeReplayed() throws Exception {
        LicenseResult result = validateAgainst(200,
                request -> store.responseFor(request, true, "active", null, null, "old-nonce"));

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void tamperedValidFlagBreaksTheSignature() throws Exception {
        LicenseResult result = validateAgainst(200,
                request -> store.responseFor(request, false, "revoked").replace("\"valid\":false", "\"valid\":true"));

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    // ── La petición ─────────────────────────────────────────────────────────

    @Test
    void licenseResourceAndAFreshNonceAreSentAsFormParameters() throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(200, "{\"valid\":true}")) {

            provider(fixture.endpoint()).validate("KOFI-9", "rpgroll-magic");
            String first = TestSigner.parseForm(fixture.lastRequestBody()).get("nonce");

            assertTrue(fixture.lastRequestBody().startsWith("license=KOFI-9&resource=rpgroll-magic&nonce="));
            assertTrue(first.matches("[0-9a-f]{32}"), first);

            provider(fixture.endpoint()).validate("KOFI-9", "rpgroll-magic");
            String second = TestSigner.parseForm(fixture.lastRequestBody()).get("nonce");

            assertTrue(!first.equals(second), "cada petición lleva su propio nonce");
        }
    }

    @Test
    void serverIdAndNameAreSentWhenKnownAndTheServerIsSigned() throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.respondingWith(200,
                request -> store.responseFor(request, true, "active"))) {

            LicenseResult result = new SelfHostedLicenseProvider(fixture.endpoint(), "srv-123", "Servidor de Pepe",
                    store.publicKey()).validate("KOFI-9", "rpgroll");

            Map<String, String> form = TestSigner.parseForm(fixture.lastRequestBody());

            assertEquals("srv-123", form.get("server"));
            assertEquals("Servidor de Pepe", form.get("server_name"));
            assertEquals(LicenseResult.Status.VALID, result.status());
            assertEquals("srv-123", result.proof().server());
        }
    }

    @Test
    void telemetryFieldsAreOmittedWhenAbsent() throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(200, "{\"valid\":true}")) {

            new SelfHostedLicenseProvider(fixture.endpoint(), null, "  ", store.publicKey())
                    .validate("KOFI-9", "rpgroll");

            Map<String, String> form = TestSigner.parseForm(fixture.lastRequestBody());

            assertTrue(!form.containsKey("server") && !form.containsKey("server_name"));
        }
    }

    // ── Fallos ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 404, 403})
    void serverErrorsAreUnknownSoBuyersAreNotBlockedByAnOutage(int statusCode) throws Exception {
        assertEquals(LicenseResult.Status.UNKNOWN, validateAgainst(statusCode, "{\"valid\":false}").status());
    }

    @Test
    void unreachableServerIsUnknown() {
        LicenseResult result = provider(LicenseHttpFixture.unreachableEndpoint()).validate("KEY", "rpgroll");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void malformedBodyIsUnknown() throws Exception {
        assertEquals(LicenseResult.Status.UNKNOWN, validateAgainst(200, "<html>oops</html>").status());
    }

    @Test
    void bodyWithoutValidFieldIsUnknown() throws Exception {
        assertEquals(LicenseResult.Status.UNKNOWN, validateAgainst(200, "{\"status\":\"active\"}").status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void missingEndpointIsInvalidWithAnActionableMessage(String endpoint) {
        LicenseResult result = provider(endpoint).validate("KEY", "rpgroll");

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("endpoint"));
    }

    @Test
    void nullEndpointIsInvalid() {
        assertEquals(LicenseResult.Status.INVALID, provider(null).validate("KEY", "rpgroll").status());
    }

    @Test
    void malformedEndpointUrlIsInvalid() {
        LicenseResult result = provider("no es una url").validate("KEY", "rpgroll");

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    @Test
    void theCompiledPublicKeyDecodes() {
        assertNotNull(LicenseSettings.signingPublicKey(),
                "SIGNING_PUBLIC_KEY no es una clave Ed25519 válida: ningún jar podría verificar nada");
    }
}
