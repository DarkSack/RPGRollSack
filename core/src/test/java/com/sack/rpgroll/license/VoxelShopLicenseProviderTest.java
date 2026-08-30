package com.sack.rpgroll.license;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxelShopLicenseProviderTest {

    private LicenseResult validateAgainst(String body) throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(200, body)) {
            return new VoxelShopLicenseProvider(fixture.endpoint()).validate("KEY", "1234");
        }
    }

    @Test
    void confirmedPurchaseIsValid() throws Exception {
        LicenseResult result = validateAgainst("""
                {"response":{"success":true,"resource":{"id":"4","purchaseValid":true,
                 "purchaseStatus":"Confirmed","purchaseTime":1780804532},"user":{"id":418054}}}""");

        assertEquals(LicenseResult.Status.VALID, result.status());
        assertTrue(result.message().contains("Confirmed"));
    }

    @Test
    void unconfirmedPurchaseIsInvalid() throws Exception {
        LicenseResult result = validateAgainst("""
                {"response":{"success":true,"resource":{"id":"4","purchaseValid":false,
                 "purchaseStatus":"Refunded"}}}""");

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("Refunded"));
    }

    @Test
    void licenseAndResourceAreSentAsFormParameters() throws Exception {
        try (LicenseHttpFixture fixture = LicenseHttpFixture.responding(200,
                "{\"response\":{\"success\":true,\"resource\":{\"purchaseValid\":true}}}")) {

            new VoxelShopLicenseProvider(fixture.endpoint()).validate("ABC-123", "77");

            assertEquals("license=ABC-123&resource_id=77", fixture.lastRequestBody());
        }
    }

    // La API real devuelve errors DENTRO de response y global como string suelto,
    // no como array en la raíz: si el parseo asume lo segundo, revienta con una
    // RuntimeException que escapa del catch de validate() y tumba el onEnable.
    @Test
    void nestedStringErrorIsReportedWithoutCrashing() throws Exception {
        LicenseResult result = validateAgainst("""
                {"request":{"action":"internalVerifyPurchase"},"response":{"success":false,
                 "error":"BAD_PRODUCT_ID","errors":{"global":"BAD_PRODUCT_ID"}}}""");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
        assertTrue(result.message().contains("BAD_PRODUCT_ID"));
    }

    @Test
    void rootLevelArrayErrorIsAlsoUnderstood() throws Exception {
        LicenseResult result = validateAgainst("""
                {"response":{"success":false},"errors":{"global":["RATE_LIMITED"]}}""");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
        assertTrue(result.message().contains("RATE_LIMITED"));
    }

    @Test
    void malformedBodyIsUnknownNeverInvalid() throws Exception {
        assertEquals(LicenseResult.Status.UNKNOWN, validateAgainst("no soy json").status());
    }

    @Test
    void responseWithoutPurchaseDataIsUnknown() throws Exception {
        LicenseResult result = validateAgainst("{\"response\":{\"success\":true,\"resource\":{}}}");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }

    @Test
    void unreachableServiceIsUnknownSoGracePeriodCanApply() {
        LicenseResult result = new VoxelShopLicenseProvider(LicenseHttpFixture.unreachableEndpoint())
                .validate("KEY", "1");

        assertEquals(LicenseResult.Status.UNKNOWN, result.status());
    }
}
