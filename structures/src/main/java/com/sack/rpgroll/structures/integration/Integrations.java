package com.sack.rpgroll.structures.integration;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Registro de integraciones opcionales: cada una está activa si integrations.yml no la apaga, el plugin está
 * habilitado y su comprobación extra (probe) no pone pegas. Un adaptador cuya reflexión falla la marca rota.
 */
public final class Integrations {

    public static final String REASON_DISABLED = "integrations.reason.disabled";
    public static final String REASON_MISSING = "integrations.reason.missing";
    public static final String REASON_BROKEN = "integrations.reason.broken";
    public static final String REASON_NO_ECONOMY = "integrations.reason.no-economy";

    /** {@code reasonKey} es null si está activa. */
    public record Status(Hook hook, boolean enabled, boolean present, boolean active, String reasonKey) {
    }

    private final Logger log;
    private final Map<Hook, Status> status = new EnumMap<>(Hook.class);

    private Integrations(Logger log) {
        this.log = log;
    }

    /**
     * @param probes comprobación extra por hook: null si va bien o la clave del motivo; si lanza, cuenta como rota
     */
    public static Integrations load(ConfigurationSection yml, Predicate<String> pluginEnabled,
                                    Map<Hook, Supplier<String>> probes, Logger log) {
        Integrations in = new Integrations(log);
        for (Hook h : Hook.values()) {
            boolean enabled = yml == null || yml.getBoolean(h.key() + ".enabled", true);
            boolean present = pluginEnabled.test(h.plugin());
            String reason = !enabled ? REASON_DISABLED : !present ? REASON_MISSING : null;
            Supplier<String> probe = probes.get(h);
            if (reason == null && probe != null) {
                try {
                    reason = probe.get();
                } catch (RuntimeException | LinkageError e) {
                    log.warning("✘ La comprobación de " + h.plugin() + " falló (" + e + "): integración apagada.");
                    reason = REASON_BROKEN;
                }
            }
            in.status.put(h, new Status(h, enabled, present, reason == null, reason));
        }
        return in;
    }

    public synchronized boolean active(Hook h) {
        return status.get(h).active();
    }

    /** Activada en el yml y con el plugin, pero su comprobación o su adaptador fallaron. */
    public synchronized boolean broken(Hook h) {
        Status st = status.get(h);
        return st.enabled() && st.present() && REASON_BROKEN.equals(st.reasonKey());
    }

    public synchronized List<Status> report() {
        return new ArrayList<>(status.values());
    }

    /** Un adaptador cuya reflexión falló: la integración se apaga y se avisa una sola vez. */
    public synchronized void markBroken(Hook h, String detail) {
        Status old = status.get(h);
        if (REASON_BROKEN.equals(old.reasonKey())) {
            return;
        }
        status.put(h, new Status(h, old.enabled(), old.present(), false, REASON_BROKEN));
        log.warning("✘ No se pudo hablar con " + h.plugin() + " (" + detail + "): integración apagada.");
    }
}
