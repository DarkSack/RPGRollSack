package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.integration.Integrations;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Une varias protecciones y elige las de las integraciones activas. */
public final class AreaGuards {

    static final String ERROR = "protect.error";

    private AreaGuards() {
    }

    /** La primera que niega gana. */
    public static AreaGuard of(List<AreaGuard> guards) {
        return (owner, world, box) -> {
            for (AreaGuard g : guards) {
                Optional<AreaGuard.Denial> d = g.check(owner, world, box);
                if (d.isPresent()) {
                    return d;
                }
            }
            return Optional.empty();
        };
    }

    /** WorldGuard y GriefPrevention, cada uno solo si su integración está activa en ese momento. */
    public static AreaGuard fromIntegrations(Supplier<Integrations> integrations) {
        return fromIntegrations(integrations, Map.of(Hook.WORLDGUARD, new WorldGuardGuard(),
                Hook.GRIEFPREVENTION, new GriefPreventionGuard()));
    }

    /**
     * Lee el registro ACTUAL en cada comprobación (una recarga lo cambia). Falla cerrado, no se construye en lo
     * dudoso: un adaptador que lanza niega con {@code protect.error} y queda roto, y una integración rota (activada y
     * con el plugin) sigue negando con {@code protect.error} sin llamar al adaptador hasta que una recarga la arregle.
     */
    static AreaGuard fromIntegrations(Supplier<Integrations> integrations, Map<Hook, AreaGuard> adapters) {
        Map<Hook, AreaGuard> ordered = new EnumMap<>(adapters);
        return (owner, world, box) -> {
            Integrations in = integrations.get();
            for (Map.Entry<Hook, AreaGuard> e : ordered.entrySet()) {
                if (in.broken(e.getKey())) {
                    return Optional.of(new AreaGuard.Denial(e.getKey(), ERROR));
                }
                if (!in.active(e.getKey())) {
                    continue;
                }
                Optional<AreaGuard.Denial> d;
                try {
                    d = e.getValue().check(owner, world, box);
                } catch (RuntimeException | LinkageError ex) {
                    in.markBroken(e.getKey(), ex.toString());
                    return Optional.of(new AreaGuard.Denial(e.getKey(), ERROR));
                }
                if (d.isPresent()) {
                    return d;
                }
            }
            return Optional.empty();
        };
    }
}
