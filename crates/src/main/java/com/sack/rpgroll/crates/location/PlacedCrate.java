package com.sack.rpgroll.crates.location;

import java.util.Objects;

/**
 * Una ubicación física en el mundo donde se registró un Crate. Varias
 * PlacedCrate pueden compartir el mismo crateId (mismo tipo de crate,
 * distintos puestos, ej. en un lobby).
 */
public record PlacedCrate(
        String placementId,
        String crateId,
        String world,
        int x,
        int y,
        int z) {

    public PlacedCrate {
        Objects.requireNonNull(placementId, "placementId no puede ser null");
        Objects.requireNonNull(crateId, "crateId no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
    }

    /** Nombre estable usado para registrar el holograma en DecentHolograms. */
    public String hologramName() {
        return "rpgroll-crate-" + placementId;
    }

}
