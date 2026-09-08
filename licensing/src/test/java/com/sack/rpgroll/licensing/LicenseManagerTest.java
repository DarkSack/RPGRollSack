package com.sack.rpgroll.licensing;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LicenseManagerTest {

    /** Id de producto de prueba: en producción lo aporta cada módulo. */
    private static final String RESOURCE = "test-resource";

    /** Slug del mismo producto en la tienda propia. Distinto a propósito. */
    private static final String SLUG = "test-slug";

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
    private static class StubProvider implements LicenseProvider {

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
        writeLicense("key: '%%__LICENSE__%%'\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("no debería llamarse"));

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG, provider).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
        assertNull(provider.seenKey);
    }

    @Test
    void blankKeyIsRejected() throws Exception {
        writeLicense("key: ''\n");

        assertEquals(LicenseResult.Status.INVALID, new LicenseManager(plugin, RESOURCE, SLUG).check().status());
    }

    @Test
    void missingKeyIsRejected() throws Exception {
        writeLicense("# sin campo key\n");

        assertEquals(LicenseResult.Status.INVALID, new LicenseManager(plugin, RESOURCE, SLUG).check().status());
    }

    @Test
    void surroundingWhitespaceInTheKeyIsTolerated() throws Exception {
        writeLicense("key: '  REAL-KEY  '\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("ok"));

        new LicenseManager(plugin, RESOURCE, SLUG, provider).check();

        assertEquals("REAL-KEY", provider.seenKey);
    }

    // El id de producto es constante de compilación: si saliera del YAML, alguien
    // que compró un módulo podría validar otro cambiando una línea.
    @Test
    void resourceIdComesFromBuildSettingsNotFromTheYaml() throws Exception {
        writeLicense("key: 'REAL-KEY'\nresource-id: 'producto-que-no-compre'\n");
        StubProvider provider = new StubProvider(LicenseResult.valid("ok"));

        new LicenseManager(plugin, RESOURCE, SLUG, provider).check();

        assertEquals(RESOURCE, provider.seenResourceId);
    }

    // Mismo motivo: el canal lo decide la forma de la clave, no un campo editable.
    // Un 'provider'/'endpoint' en el YAML no debe tener ningún efecto.
    @Test
    void providerAndEndpointFieldsInTheYamlAreIgnored() throws Exception {
        writeLicense("""
                key: 'REAL-KEY'
                provider: self-hosted
                endpoint: 'http://servidor-del-pirata.example/verify'
                """);
        StubProvider provider = new StubProvider(LicenseResult.valid("ok"));

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG, provider).check();

        // Llega al stub tal cual, sin que el YAML haya podido redirigir nada.
        assertEquals("REAL-KEY", provider.seenKey);
        assertEquals(LicenseResult.Status.VALID, result.status());
    }

    // El enrutamiento se comprueba sobre el proveedor elegido, no disparando una
    // verificación real: un test unitario no debe depender de la red ni del
    // endpoint que tenga configurado el build.
    @Test
    void selfHostedKeyPrefixSelectsTheSelfHostedProvider() {
        LicenseProvider provider = new LicenseManager(plugin, RESOURCE, SLUG)
                .resolveProvider(LicenseSettings.SELF_HOSTED_KEY_PREFIX + "AAAAA-BBBBB");

        assertInstanceOf(SelfHostedLicenseProvider.class, provider);
    }

    @Test
    void aNonPrefixedKeyGoesToVoxelShop() {
        LicenseProvider provider = new LicenseManager(plugin, RESOURCE, SLUG).resolveProvider("VOXEL-STYLE-KEY");

        assertInstanceOf(VoxelShopLicenseProvider.class, provider);
    }

    // Los dos canales tienen catálogos separados y ninguno reconoce el
    // identificador del otro. Cuando ambos recibían el id de voxel.shop, la
    // tienda propia respondía "not-covered" — que es valid:false, o sea
    // licencia inválida y sin período de gracia: el plugin de alguien que pagó
    // no arrancaba, y el log culpaba a su licencia.
    @Test
    void theSelfHostedChannelGetsTheSlugAndVoxelShopTheMarketplaceId() {
        LicenseManager manager = new LicenseManager(plugin, RESOURCE, SLUG);

        assertEquals(SLUG, manager.identifierFor(new SelfHostedLicenseProvider("https://x/api/verify")),
                "la tienda propia identifica los productos por slug");
        assertEquals(RESOURCE, manager.identifierFor(new VoxelShopLicenseProvider()),
                "voxel.shop identifica los productos por su id numérico");
    }

    @Test
    void theSelfHostedRequestCarriesTheSlug() throws Exception {
        writeLicense("key: '" + LicenseSettings.SELF_HOSTED_KEY_PREFIX + "AAAAA'\n");

        StubProvider provider = new StubProvider(LicenseResult.valid("ok")) {
            @Override
            public boolean usesProductSlug() {
                return true;
            }
        };

        new LicenseManager(plugin, RESOURCE, SLUG, provider).check();

        assertEquals(SLUG, provider.seenResourceId);
    }

    @Test
    void theSelfHostedEndpointIsConfiguredInTheBuild() {
        // Si queda vacío, toda venta directa falla al arrancar el servidor.
        assertFalse(LicenseSettings.SELF_HOSTED_ENDPOINT.isBlank(),
                "SELF_HOSTED_ENDPOINT sin configurar en LicenseSettings");
    }

    /**
     * El endpoint tiene que estar en un dominio propio.
     * <p>
     * Esta constante queda compilada en cada jar, así que es la única dirección
     * que una copia vendida sabe consultar — para siempre. Apuntarla al
     * subdominio gratuito de un proveedor significa que el día que ese
     * subdominio desaparezca, todos los jars que ya están en el disco de los
     * compradores dejan de validar y no hay arreglo posible.
     * <p>
     * Es fácil que alguien lo apunte "un momento" a un despliegue de prueba y
     * se le escape a una publicación, porque nada más lo delata: compila,
     * arranca y valida igual mientras ese despliegue exista.
     */
    @Test
    void theSelfHostedEndpointIsNotOnAProviderSubdomain() {
        String endpoint = LicenseSettings.SELF_HOSTED_ENDPOINT;

        for (String provider : new String[] {".vercel.app", ".netlify.app", ".onrender.com",
                ".herokuapp.com", ".fly.dev", ".pages.dev", "localhost", "127.0.0.1"}) {
            assertFalse(endpoint.contains(provider),
                    "SELF_HOSTED_ENDPOINT apunta a " + provider + ", que no es un dominio propio: "
                            + endpoint);
        }

        assertTrue(endpoint.startsWith("https://"),
                "la verificación tiene que ir por HTTPS: " + endpoint);
    }

    @Test
    void validationSuccessIsCachedForTheGracePeriod() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");

        new LicenseManager(plugin, RESOURCE, SLUG, new StubProvider(LicenseResult.valid("ok"))).check();

        assertTrue(new File(dataFolder, ".license-cache.yml").exists());
    }

    @Test
    void outageFallsBackToARecentSuccessfulValidation() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");
        writeCache(true, System.currentTimeMillis());

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.VALID, result.status());
    }

    @Test
    void outageWithAnExpiredCacheIsRejected() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");
        writeCache(true, System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000));

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    @Test
    void outageWithNoCacheAtAllIsRejected() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    // Una revocación NO debe poder rescatarse con el caché de gracia.
    @Test
    void revocationIsNotRescuedByTheGracePeriodCache() throws Exception {
        writeLicense("key: 'RPGR-KOFI-KEY'\n");
        writeCache(true, System.currentTimeMillis());

        LicenseResult result = new LicenseManager(plugin, RESOURCE, SLUG,
                new StubProvider(LicenseResult.invalid("revocada"))).check();

        assertEquals(LicenseResult.Status.INVALID, result.status());
    }

    // Un rechazo invalida el caché, para que no quede una gracia utilizable
    // en el próximo arranque si el servidor pasa a estar inalcanzable.
    @Test
    void rejectionOverwritesTheCacheAsInvalid() throws Exception {
        writeLicense("key: 'REAL-KEY'\n");
        writeCache(true, System.currentTimeMillis());

        new LicenseManager(plugin, RESOURCE, SLUG, new StubProvider(LicenseResult.invalid("revocada"))).check();

        LicenseResult afterOutage = new LicenseManager(plugin, RESOURCE, SLUG,
                new StubProvider(LicenseResult.unknown("servidor caído"))).check();

        assertEquals(LicenseResult.Status.INVALID, afterOutage.status());
    }
}
