package com.sack.rpgroll.workers.core.worker;

import org.bukkit.NamespacedKey;

/** Claves de PersistentDataContainer para marcar una entidad real como un worker rastreado. */
public final class WorkerKeys {

    public static final NamespacedKey TRACKED = new NamespacedKey("rpgrollworkers", "tracked");
    public static final NamespacedKey PROFESSION_ID = new NamespacedKey("rpgrollworkers", "profession-id");

    private WorkerKeys() {
    }

}
