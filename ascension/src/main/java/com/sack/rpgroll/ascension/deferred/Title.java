package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/** Un título cosmético que el jugador puede desbloquear y mostrar (ej. "El Elegido"). */
public record Title(String id, String displayName) implements RPGContent {

    public Title {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

}
