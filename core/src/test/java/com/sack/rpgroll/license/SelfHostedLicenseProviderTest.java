package com.sack.rpgroll.license;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfHostedLicenseProviderTest {

    private LicenseResult validateAgainst(int statusCode, String body) throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(statusCode, body)) {
            return new SelfHostedLicenseProvider(fixture.endpoint()).validate("KEY", "rpgroll");
        }
    }

    @Test
    void activeLicenseIsValid() throws Exception {
        LicenseResult result = validateAgainst(200, "{\"valid\":true,\"status\":\"active\"}");

        assertEquals(LicenseResult.Status.VALID, result.status());
    }

    // El punto del canal propio: revocar una clave la bloquea de inmediato,
    // sin período de gracia (a diferencia de una caída del servidor).
    @Test
    void revokedLicenseIsInvalidNotUnknown() throws Exception {
        LicenseResult result = validateAgainst(200, "{\"valid\":false,\"status\":\"revoked\"}");

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("revoked"));
    }

    @Test
    void serverMessageIsPreferredOverTheGenericOne() throws Exception {
        LicenseResult result = validateAgainst(200,
                "{\"valid\":false,\"status\":\"revoked\",\"message\":\"Reembolsada el 2026-08-01\"}");

        assertEquals("Reembolsada el 2026-08-01", result.message());
    }

    @Test
    void licenseAndResourceAreSentAsFormParameters() throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(200, "{\"valid\":true}")) {

            new SelfHostedLicenseProvider(fixture.endpoint()).validate("KOFI-9", "rpgroll-magic");

            assertEquals("license=KOFI-9&resource=rpgroll-magic", fixture.lastRequestBody());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 404, 403})
    void serverErrorsAreUnknownSoBuyersAreNotBlockedByAnOutage(int statusCode) throws Exception {
        assertEquals(LicenseResult.Status.UNKNOWN, validateAgainst(statusCode, "{\"valid\":false}").status());
    }

    @Test
    void unreachableServerIsUnknown() {
        LicenseResult result = new SelfHostedLicenseProvider(LicenseHttpFixture.unreachableEndpoint())
                .validate("KEY", "rpgroll");

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
        LicenseResult result = new SelfHostedLicenseProvider(endpoint).validate("KEY", "rpgroll");

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("endpoint"));
    }

    @Test
    void nullEndpointIsInvalid() {
        assertEquals(LicenseResult.Status.INVALID,
                new SelfHostedLicenseProvider(null).validate("KEY", "rpgroll").status());
    }

    @Test
    void malformedEndpointUrlIsInvalid() {
        LicenseResult result = new SelfHostedLicenseProvider("no es una url").validate("KEY", "rpgroll");

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }
}
