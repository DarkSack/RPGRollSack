package com.sack.rpgroll.license;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LicenseManagerTest {

    @TempDir
    File dataFolder;

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LicenseManagerTest"));
    }

    private void writeLicense(String contents) throws IOException {
        Files.writeString(new File(dataFolder, "license.yml").toPath(), contents, StandardCharsets.UTF_8);
    }

    private void writeCache(boolean valid, long validatedAt) throws IOException {
        Files.writeString(new File(dataFolder, ".license-cache.yml").toPath(),
                "valid: " + valid + "\nlast-validated-at: " + validatedAt + "\n", StandardCharsets.UTF_8);
    }

    /** Proveedor de prueba que devuelve siempre lo mismo y recuerda lo que recibió. */
    private static final class StubProvider implements LicenseProvider {

        private final LicenseResult result;
        private String seenKey;
        private String seenResourceId;

        StubProvider(LicenseResult result) {
            this.result = result;
        }

        @Override
        public LicenseResult validate(String licenseKey, String resourceId) {
            this.seenKey = licenseKey;
            this.seenResourceId = resourceId;
            return result;
        }

        @Override
        public String name() {
            return "stub";
        }
    }

    @Test
    void untouchedMarketplacePlaceholderIsRejectedWithoutCallingTheProvider() throws Exception {
        writeLicense("provider: voxel-shop\nkey: '%%__LICENSE__%%'\nresource-id: '1'\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("no debería llamarse"));

        LicenseResult result = new LicenseManager(plugin, provider).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertEquals(null, provider.seenKey);
    }

    @Test
    void blankKeyIsRejected() throws Exception {
        writeLicense("provider: voxel-shop\nkey: ''\nresource-id: '1'\n");

        assertEquals(LicenseResult.Status.INVALID, new LicenseManager(plugin).check().status());
    }

    @Test
    void keyAndResourceIdReachTheProvider() throws Exception {
        writeLicense("provider: voxel-shop\nkey: 'REAL-KEY'\nresource-id: '4321'\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("ok"));

        new LicenseManager(plugin, provider).check();

        assertEquals("REAL-KEY", provider.seenKey);
        assertEquals("4321", provider.seenResourceId);
    }

    @Test
    void unknownProviderNameIsRejectedWithAnActionableMessage() throws Exception {
        writeLicense("provider: mercadolibre\nkey: 'REAL-KEY'\n");

        LicenseResult result = new LicenseManager(plugin).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("voxel-shop"));
    }

    @Test
    void selfHostedWithoutEndpointIsRejected() throws Exception {
        writeLicense("provider: self-hosted\nkey: 'KOFI-KEY'\nendpoint: ''\n");

        LicenseResult result = new LicenseManager(plugin).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertTrue(result.message().contains("endpoint"));
    }

    // Con venta directa la clave se pega a mano, así que el placeholder del
    // marketplace no debe seguir siendo un caso especial más allá de rechazarlo.
    @Test
    void selfHostedAcceptsAManuallyPastedKey() throws Exception {
        writeLicense("provider: self-hosted\nkey: 'KOFI-KEY'\nendpoint: 'http://example.invalid/verify'\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("ok"));

        new LicenseManager(plugin, provider).check();

        assertEquals("KOFI-KEY", provider.seenKey);
    }

    @Test
    void validationSuccessIsCachedForTheGracePeriod() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");

        new LicenseManager(plugin, new StubProvider(LicenseResult.valid("ok"))).check();

        assertTrue(new File(dataFolder, ".license-cache.yml").exists());
    }

    @Test
    void outageFallsBackToARecentSuccessfulValidation() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");
        writeCache(true, System.currentTimeMillis());

        LicenseResult result = new LicenseManager(plugin,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.VALID, result.status());
    }

    @Test
    void outageWithAnExpiredCacheIsRejected() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");
        writeCache(true, System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000));

        LicenseResult result = new LicenseManager(plugin,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    @Test
    void outageWithNoCacheAtAllIsRejected() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");

        LicenseResult result = new LicenseManager(plugin,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    // Una revocación NO debe poder rescatarse con el caché de gracia.
    @Test
    void revocationIsNotRescuedByTheGracePeriodCache() throws Exception {
        writeLicense("provider: self-hosted\nkey: 'KOFI-KEY'\nendpoint: 'http://example.invalid/verify'\n");
        writeCache(true, System.currentTimeMillis());

        LicenseResult result = new LicenseManager(plugin,
                new StubProvider(LicenseResult.invalid("revocada"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    @Test
    void polymartIsAcceptedAsAnAliasOfVoxelShop() throws Exception {
        writeLicense("provider: polymart\nkey: 'REAL-KEY'\nresource-id: '1'\n");

        // Sin override llega al proveedor real; basta con que NO sea el rechazo
        // por nombre desconocido para confirmar que el alias se resolvió.
        LicenseResult result = new LicenseManager(plugin).check();

        assertTrue(!result.message().contains("no reconoce el valor"));
    }
}
