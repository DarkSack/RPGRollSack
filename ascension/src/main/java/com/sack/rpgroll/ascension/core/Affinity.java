package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Una afinidad elemental (fuego, agua, hielo, etc.). {@code resistCauses}
 * mapea a nombres de {@link org.bukkit.event.entity.EntityDamageEvent.DamageCause}
 * — el nivel de afinidad del jugador reduce el daño de esas causas. Es el
 * único gancho mecánico real de este sistema; el resto (bonos a
 * encantamientos/hechizos/objetos) queda expuesto vía API para que esos
 * addons lo consulten.
 */
public record Affinity(String id, String displayName, String opposingId, List<String> resistCauses)
        implements RPGContent {

    public Affinity {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        resistCauses = resistCauses == null ? List.of() : List.copyOf(resistCauses);
    }

}
