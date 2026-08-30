package com.sack.rpgroll.license;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.common.resource.ResourceFile;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;

/**
 * Verifica que el servidor esté corriendo una copia legítimamente
 * comprada de RPGRoll antes de que el resto del plugin arranque.
 * <p>
 * <b>Lo único que aporta el comprador es la clave.</b> A qué servicio se
 * consulta y qué producto se valida son constantes de compilación
 * ({@link LicenseSettings}) — si fueran configurables, alcanzaría con
 * editar un YAML para apuntar la verificación a un servidor complaciente.
 * <p>
 * Soporta los dos canales de venta con un mismo jar, distinguiéndolos por
 * la forma de la clave:
 * <ul>
 *   <li>voxel.shop sustituye el placeholder {@code %%__LICENSE__%%} por la
 *       clave del comprador al momento de la descarga. Si el placeholder
 *       sigue intacto, es una descarga directa/no comprada.</li>
 *   <li>Las ventas propias (Ko-fi, Patreon) usan claves emitidas por el
 *       servidor propio, reconocibles por su prefijo
 *       {@link LicenseSettings#SELF_HOSTED_KEY_PREFIX}, que el comprador
 *       pega a mano.</li>
 * </ul>
 */
public class LicenseManager {

    private static final String PLACEHOLDER = "%%__LICENSE__%%";

    private final Plugin plugin;
    private final LicenseProvider providerOverride;
    private final LicenseCache cache;

    public LicenseManager(Plugin plugin) {
        this(plugin, null);
    }

    /** Constructor para tests o para forzar un proveedor concreto. */
    public LicenseManager(Plugin plugin, LicenseProvider providerOverride) {
        this.plugin = plugin;
        this.providerOverride = providerOverride;
        this.cache = new LicenseCache(plugin);
    }

    public LicenseResult check() {

        File licenseFile = loadLicenseFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(licenseFile);

        String key = config.getString("key", PLACEHOLDER);

        if (key == null || key.isBlank()) {
            return LicenseResult.invalid("license.yml no tiene ninguna clave en el campo 'key'.");
        }

        key = key.trim();

        if (key.equals(PLACEHOLDER)) {
            return LicenseResult.invalid(
                    "No se detectó una licencia — esta parece ser una descarga directa, no una compra verificada.");
        }

        LicenseProvider provider = providerOverride != null ? providerOverride : resolveProvider(key);

        plugin.getLogger().info("Verificando licencia contra " + provider.name() + "...");

        LicenseResult result = provider.validate(key, LicenseSettings.RESOURCE_ID);

        return switch (result.status()) {

            case VALID -> {
                cache.write(true);
                yield result;
            }

            case INVALID -> {
                cache.write(false);
                yield result;
            }

            case UNKNOWN -> handleUnknown(result);
        };
    }

    /** El canal lo dice la clave, no la configuración — ver {@link LicenseSettings}. */
    private LicenseProvider resolveProvider(String key) {
        return key.startsWith(LicenseSettings.SELF_HOSTED_KEY_PREFIX)
                ? new SelfHostedLicenseProvider(LicenseSettings.SELF_HOSTED_ENDPOINT)
                : new VoxelShopLicenseProvider();
    }

    private LicenseResult handleUnknown(LicenseResult networkFailure) {

        var cached = cache.read();

        if (cached.isPresent() && cache.isWithinGracePeriod(cached.get())) {
            plugin.getLogger().warning("✘ " + networkFailure.message()
                    + " — usando la última validación exitosa reciente (período de gracia).");
            return LicenseResult.valid("Período de gracia activo tras un fallo de red.");
        }

        return LicenseResult.invalid("No se pudo validar la licencia y no hay una validación previa reciente: "
                + networkFailure.message());
    }

    private File loadLicenseFile() {

        new DirectoryCreator(plugin).create(List.of());
        new ResourceCopier(plugin).copyFiles(List.of(new ResourceFile("license/license.yml", "license.yml", true)));

        return new File(plugin.getDataFolder(), "license.yml");
    }

}
