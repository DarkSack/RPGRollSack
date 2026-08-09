package com.sack.rpgroll.workers.core.logistics;

import org.bukkit.Location;

import java.util.Objects;

/**
 * Un cofre/baúl designado como almacén — no es contenido admin-creado
 * (como {@code Species} en Ranching), es una ubicación real del mundo
 * marcada por un jugador con la herramienta designadora. Reconoce
 * cualquier {@code Container} vanilla (cofre, barril, hopper...), sin
 * distinguir silo/granero/heladera como tipos separados — esa distinción
 * del diseño original es puramente estética, no cambia el comportamiento.
 */
public record Warehouse(Location location, String resourceFilter) {

    public Warehouse {
        Objects.requireNonNull(location, "location no puede ser null");
        resourceFilter = resourceFilter == null ? "" : resourceFilter.toLowerCase(java.util.Locale.ROOT);
    }

    /** Vacío = acepta cualquier recurso. */
    public boolean accepts(String materialName) {
        return resourceFilter.isBlank() || materialName.toLowerCase(java.util.Locale.ROOT).contains(resourceFilter);
    }

}
