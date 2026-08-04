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
 * La clave de licencia NUNCA vive hardcodeada en el código ni en el
 * repositorio: {@code license.yml} se distribuye con el placeholder
 * literal {@code %%__LICENSE__%%}, que Polymart reemplaza automáticamente
 * por la clave real del comprador al momento de la descarga. Si el
 * placeholder sigue intacto, es una descarga directa/no comprada.
 */
public class LicenseManager {

    private static final String PLACEHOLDER = "%%__LICENSE__%%";

    private final Plugin plugin;
    private final LicenseProvider provider;
    private final LicenseCache cache;

    public LicenseManager(Plugin plugin) {
        this(plugin, new PolymartLicenseProvider());
    }

    /** Constructor para tests o para usar otro marketplace sin tocar esta clase. */
    public LicenseManager(Plugin plugin, LicenseProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
        this.cache = new LicenseCache(plugin);
    }

    public LicenseResult check() {

        File licenseFile = loadLicenseFile();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(licenseFile);

        String key = config.getString("key", PLACEHOLDER);
        String resourceId = config.getString("resource-id", "0");

        if (key == null || key.isBlank() || key.equals(PLACEHOLDER)) {
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
