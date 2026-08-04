package com.sack.rpgroll.ranching.core.genetics;

/** Qué hace una mutación cuando se dispara al nacer una cría. */
public enum MutationEffectType {

    /** Multiplica el valor de fenotipo ya resuelto del gen afectado (ej. "Producción doble" -> x2). */
    MULTIPLY,

    /** Reemplaza el valor de fenotipo del gen afectado por un valor fijo (ej. "Gigantismo" -> peso al máximo). */
    OVERRIDE,

    /** Puramente cosmética/etiqueta — no toca ningún gen (ej. "Lana dorada", "Ojos azules"). */
    COSMETIC_TAG
}
