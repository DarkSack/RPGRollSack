package com.sack.rpgroll.license;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Recuerda la última validación exitosa en disco. Si Polymart no responde
 * (caída temporal, sin internet un instante), se le da al comprador un
 * período de gracia en vez de bloquear el plugin — pero si pasa
 * {@link #GRACE_PERIOD_MILLIS} sin poder revalidar, se vuelve a exigir una
 * verificación real.
 */
public class LicenseCache {

    private static final long GRACE_PERIOD_MILLIS = 7L * 24 * 60 * 60 * 1000; // 7 días

    private final File file;

    public LicenseCache(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), ".license-cache.yml");
    }

    public record CachedState(boolean valid, long lastValidatedAt) {
    }

    public Optional<CachedState> read() {

        if (!file.exists()) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("last-validated-at")) {
            return Optional.empty();
        }

        return Optional.of(new CachedState(config.getBoolean("valid", false), config.getLong("last-validated-at")));
    }

    public void write(boolean valid) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("valid", valid);
        config.set("last-validated-at", System.currentTimeMillis());

        try {
            config.save(file);
        } catch (IOException ignored) {
            // No poder escribir el caché no es crítico — en el peor caso, se
            // vuelve a validar contra la red en el próximo arranque.
        }
    }

    public boolean isWithinGracePeriod(CachedState state) {
        return state.valid() && (System.currentTimeMillis() - state.lastValidatedAt()) < GRACE_PERIOD_MILLIS;
    }

}
