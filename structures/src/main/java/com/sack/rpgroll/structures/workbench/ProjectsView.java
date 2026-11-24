package com.sack.rpgroll.structures.workbench;

import java.util.UUID;

/** Pregunta si una mesa tiene una obra activa. El plan 3 pone la implementación real. */
public interface ProjectsView {
    boolean hasActiveProject(UUID bench);

    /** Hasta el plan 3 ninguna mesa tiene obras. */
    static ProjectsView none() {
        return bench -> false;
    }
}
