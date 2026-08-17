package com.sack.rpgroll.mobs.core;

import com.sack.rpgroll.common.reskin.EntityReskin;

import java.util.Objects;

/**
 * Una skin sorteable de un mob — envuelve un {@link EntityReskin} (el
 * reskin visual real: material + custom-model-data + escala) con un
 * {@code id} (para persistir cuál se sorteó, ver {@code MobInstanceService})
 * y un {@code weight} para el sorteo ponderado al spawnear.
 */
public record MobSkin(String id, EntityReskin reskin, double weight) {

    public MobSkin {
        Objects.requireNonNull(id, "id no puede ser null");
        reskin = reskin == null ? EntityReskin.NONE : reskin;
        weight = weight <= 0 ? 1.0 : weight;
    }

}
