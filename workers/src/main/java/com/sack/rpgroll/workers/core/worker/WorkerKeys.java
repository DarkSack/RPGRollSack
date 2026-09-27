package com.sack.rpgroll.workers.core.worker;

import org.bukkit.NamespacedKey;

/** Claves de PersistentDataContainer para marcar una entidad real como un worker rastreado. */
public final class WorkerKeys {

    public static final NamespacedKey TRACKED = new NamespacedKey("rpgrollworkers", "tracked");
    public static final NamespacedKey PROFESSION_ID = new NamespacedKey("rpgrollworkers", "profession-id");

    /** En el maniquí que hace de cuerpo de un worker con skin: el uuid de ese worker. */
    public static final NamespacedKey BODY = new NamespacedKey("rpgrollworkers", "body");

    /** En el mob de un worker con skin: lo escondimos nosotros (para volver a enseñarlo al quitarla). */
    public static final NamespacedKey HIDDEN = new NamespacedKey("rpgrollworkers", "hidden");

    private WorkerKeys() {
    }

}
