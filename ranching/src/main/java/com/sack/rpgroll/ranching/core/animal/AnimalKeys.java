package com.sack.rpgroll.ranching.core.animal;

import org.bukkit.NamespacedKey;

/** Claves de PersistentDataContainer usadas para marcar una entidad real como un animal rastreado por Ranching. */
public final class AnimalKeys {

    /** Marca la entidad como rastreada — el estado real vive en {@code AnimalManager}, esto es solo un filtro rápido. */
    public static final NamespacedKey TRACKED = new NamespacedKey("rpgrollranching", "tracked");
    public static final NamespacedKey SPECIES_ID = new NamespacedKey("rpgrollranching", "species-id");

    private AnimalKeys() {
    }

}
