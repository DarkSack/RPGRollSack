package com.sack.rpgroll.license;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.common.resource.ResourceFile;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Verifica que el servidor esté corriendo una copia legítimamente
 * comprada de RPGRoll antes de que el resto del plugin arranque.
 * <p>
 * Soporta dos canales de venta, elegidos con el campo {@code provider} de
 * {@code license.yml}:
 * <ul>
 *   <li>{@code voxel-shop} (por defecto) — el marketplace sustituye el
 *       placeholder {@code %%__LICENSE__%%} por la clave del comprador al
 *       descargar, así que la clave NUNCA vive en el repositorio. Si el
 *       placeholder sigue intacto, es una descarga directa/no comprada.</li>
 *   <li>{@code self-hosted} — ventas propias (Ko-fi, Patreon, directas),
 *       validadas contra el servidor de licencias del vendedor. Acá la clave
 *       se la pasa el vendedor al comprador y este la pega a mano, así que
 *       el placeholder no aplica.</li>
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

    /** Constructor para tests o para forzar un proveedor sin tocar license.yml. */
    public LicenseManager(Plugin plugin, LicenseProvider providerOverride) {
        this.plugin = plugin;
        this.providerOverride = providerOverride;
        this.cache = new LicenseCache(plugin);
    }

    public LicenseResult check() {

        File licenseFile = loadLicenseFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(licenseFile);

        String key = config.getString("key", PLACEHOLDER);
        String resourceId = config.getString("resource-id", "0");
        String providerId = config.getString("provider", "voxel-shop").trim().toLowerCase(Locale.ROOT);

        LicenseProvider provider = providerOverride != null
                ? providerOverride
                : resolveProvider(providerId, config.getString("endpoint"));

        if (provider == null) {
            return LicenseResult.invalid("El campo 'provider' de license.yml no reconoce el valor '"
                    + providerId + "' — valores válidos: voxel-shop, self-hosted.");
        }

        if (key == null || key.isBlank()) {
            return LicenseResult.invalid("license.yml no tiene ninguna clave en el campo 'key'.");
        }

        // El placeholder solo lo sustituye el marketplace: en ventas propias
        // la clave se pega a mano, así que ahí no significa nada.
        if (key.equals(PLACEHOLDER)) {
            return LicenseResult.invalid(
                    "No se detectó una licencia — esta parece ser una descarga directa, no una compra verificada.");
        }

        plugin.getLogger().info("Verificando licencia contra " + provider.name() + "...");

        LicenseResult result = provider.validate(key, resourceId);

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

    private LicenseProvider resolveProvider(String providerId, String endpoint) {
        return switch (providerId) {
            case "voxel-shop", "polymart" -> new VoxelShopLicenseProvider();
            case "self-hosted" -> new SelfHostedLicenseProvider(endpoint);
            default -> null;
        };
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
