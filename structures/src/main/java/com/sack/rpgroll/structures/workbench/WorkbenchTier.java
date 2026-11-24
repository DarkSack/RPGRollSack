package com.sack.rpgroll.structures.workbench;

import java.util.Optional;

/** Nivel de una mesa de construcción. */
public enum WorkbenchTier {
    CONSTRUCTOR(1), INGENIERO(2), ARQUITECTO(3);

    private final int level;

    WorkbenchTier(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public Optional<WorkbenchTier> next() {
        return ofLevel(level + 1);
    }

    /** Por nombre (sin importar mayúsculas); vacío si no existe. */
    public static Optional<WorkbenchTier> byName(String name) {
        for (WorkbenchTier t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static Optional<WorkbenchTier> ofLevel(int level) {
        for (WorkbenchTier t : values()) {
            if (t.level == level) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
