package com.sack.rpgroll.structures.catalog;

import java.util.List;

/** Un material de la obra: cualquiera de {@code anyOf} vale; {@code key()} es la clave estable en la base de datos. */
public record MaterialSpec(List<String> anyOf, int amount, boolean optional) {

    public String key() {
        return anyOf.getFirst();
    }
}
