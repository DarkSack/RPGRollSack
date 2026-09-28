package com.sack.rpgroll.ranching.core.animal;

import org.bukkit.NamespacedKey;

/** Claves de PersistentDataContainer usadas para marcar una entidad real como un animal rastreado por Ranching. */
public final class AnimalKeys {

    /** Marca la entidad como rastreada — el estado real vive en {@code AnimalManager}, esto es solo un filtro rápido. */
    public static final NamespacedKey TRACKED = new NamespacedKey("rpgrollranching", "tracked");
    public static final NamespacedKey SPECIES_ID = new NamespacedKey("rpgrollranching", "species-id");
    /**
     * El id del animal que representa la entidad. Sin él (animales de antes) el id es el uuid de la
     * entidad; con él, una entidad recreada tras perderse sigue apuntando al mismo animal.
     */
    /** El modelo de FreeMinecraftModels que se le puso (para devolverle la visibilidad al quitarlo). */
    public static final NamespacedKey MODEL = new NamespacedKey("rpgrollranching", "model");
    public static final NamespacedKey ANIMAL_ID = new NamespacedKey("rpgrollranching", "animal-id");

    private AnimalKeys() {
    }

}
