package com.sack.rpgroll.licensing;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Recuerda la última validación exitosa en disco. Si el proveedor no responde
 * (caída temporal, sin internet un instante), se le da al comprador un
 * período de gracia en vez de bloquear el plugin — pero si pasa
 * {@link #GRACE_PERIOD_MILLIS} sin poder revalidar, se vuelve a exigir una
 * verificación real.
 * <p>
 * Para las claves del servidor propio se guarda además la validación FIRMADA
 * ({@link LicenseProof}), y es lo único que {@link LicenseManager} acepta para
 * ellas: un {@code valid: true} escrito a mano no concede nada. Las de
 * voxel.shop no traen firma, así que siguen con la regla simple.
 */
public class LicenseCache {

    private static final long GRACE_PERIOD_MILLIS = 7L * 24 * 60 * 60 * 1000; // 7 días

    /** Adelanto tolerado en una fecha escrita por este mismo servidor. */
    private static final long LOCAL_SKEW_MILLIS = 60_000L;

    /**
     * Adelanto tolerado en una fecha firmada por el servidor de licencias: es
     * otro reloj, y uno de Minecraft mal sincronizado no debería perder la
     * gracia justo el día que la tienda se cae. Falsificarla ya no es posible.
     */
    private static final long SIGNED_SKEW_MILLIS = 24L * 60 * 60 * 1000;

    private final File file;

    public LicenseCache(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), ".license-cache.yml");
    }

    public record CachedState(boolean valid, long lastValidatedAt, LicenseProof proof) {

        public CachedState(boolean valid, long lastValidatedAt) {
            this(valid, lastValidatedAt, null);
        }
    }

    public Optional<CachedState> read() {

        if (!file.exists()) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("last-validated-at")) {
            return Optional.empty();
        }

        LicenseProof proof = null;
        ConfigurationSection section = config.getConfigurationSection("proof");

        if (section != null && section.contains("signature") && section.contains("issued-at")) {
            proof = new LicenseProof(
                    section.getString("status", ""),
                    section.getString("server", ""),
                    section.getString("nonce", ""),
                    section.getLong("issued-at"),
                    section.getString("signature", ""));
        }

        return Optional.of(new CachedState(
                config.getBoolean("valid", false), config.getLong("last-validated-at"), proof));
    }

    public void write(boolean valid) {
        write(valid, null);
    }

    public void write(boolean valid, LicenseProof proof) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("valid", valid);
        config.set("last-validated-at", System.currentTimeMillis());

        if (valid && proof != null) {
            config.set("proof.status", proof.status());
            config.set("proof.server", proof.server());
            config.set("proof.nonce", proof.nonce());
            config.set("proof.issued-at", proof.issuedAt());
            config.set("proof.signature", proof.signature());
        }

        try {
            config.save(file);
        } catch (IOException ignored) {
            // No poder escribir el caché no es crítico — en el peor caso, se
            // vuelve a validar contra la red en el próximo arranque.
        }
    }

    public boolean isWithinGracePeriod(CachedState state) {
        return isWithinGracePeriod(state, System.currentTimeMillis());
    }

    /**
     * Una validación con fecha futura no cuenta.
     * <p>
     * El archivo es YAML en la carpeta del servidor. Antes bastaba con escribir
     * {@code last-validated-at: 9999999999999} y bloquear el dominio de la
     * tienda: "ahora menos el futuro" da negativo, que siempre es menor que siete
     * días, y el plugin arrancaba en gracia para siempre sin licencia. Se tolera
     * un minuto de adelanto por relojes desajustados entre arranques.
     */
    static boolean isWithinGracePeriod(CachedState state, long now) {
        return state.valid() && withinWindow(now - state.lastValidatedAt(), LOCAL_SKEW_MILLIS);
    }

    /**
     * Gracia para una clave del servidor propio: solo con una validación firmada
     * que verifique contra esta clave y este producto, y contando desde la fecha
     * que firmó el servidor.
     */
    static boolean isWithinSignedGracePeriod(CachedState state, java.security.PublicKey publicKey,
                                             String licenseKey, String resource, long now) {
        LicenseProof proof = state.proof();

        return state.valid()
                && proof != null
                && proof.verifies(publicKey, licenseKey, resource)
                && withinWindow(now - proof.issuedAt(), SIGNED_SKEW_MILLIS);
    }

    private static boolean withinWindow(long elapsed, long skew) {
        return elapsed >= -skew && elapsed < GRACE_PERIOD_MILLIS;
    }

}
