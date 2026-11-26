package com.sack.rpgroll.structures.workbench;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Lo que la mesa necesita de sus obras, sin depender del motor ni de las devoluciones. Hilo principal. La
 * implementación real es {@link BenchProjects}.
 */
public interface ProjectsView {
    boolean hasActiveProject(UUID bench);

    /** {@code break-policy: pause}: la mesa ya salió del mundo (huérfana). Solo una obra BUILDING se pausa. */
    default void benchRemoved(Player by, UUID bench) {
    }

    /**
     * {@code break-policy: cancel}: cancela la obra activa de la mesa, con la devolución al dueño de la obra. true si la
     * base de datos lo confirma (o ya no había obra); solo entonces se rompe la mesa.
     */
    default CompletableFuture<Boolean> cancelFor(Player by, UUID bench) {
        return CompletableFuture.completedFuture(true);
    }

    /** La mesa volvió al mundo: si su obra estaba en pausa por la mesa, se reanuda con los derechos de su dueño. */
    default void benchPlaced(Player by, UUID bench) {
    }

    /** Sin obras (pruebas). */
    static ProjectsView none() {
        return bench -> false;
    }
}
