package com.sack.rpgroll.structures.protect;

import com.sack.rpgroll.structures.integration.Hook;
import com.sack.rpgroll.structures.storage.Box;

import java.util.Optional;
import java.util.UUID;

/**
 * Protección externa de la zona de una obra: se pregunta una vez por la caja entera y por el UUID del dueño (vale con
 * el dueño desconectado). Hilo principal. El jugador conectado que confirma o arranca con
 * {@code rpgrollstructures.bypass} ({@code WorkbenchService.PERM_BYPASS}) se salta la comprobación.
 */
public interface AreaGuard {

    /** Vacío si el dueño puede construir en toda la caja. */
    Optional<Denial> check(UUID owner, String world, Box box);

    /** Quién niega y la clave del mensaje ({@code protect.worldguard}, {@code protect.griefprevention}...). */
    record Denial(Hook hook, String langKey) {
    }

    static AreaGuard allowAll() {
        return (owner, world, box) -> Optional.empty();
    }
}
