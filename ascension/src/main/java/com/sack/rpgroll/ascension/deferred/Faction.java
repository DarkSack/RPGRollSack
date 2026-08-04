package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/** Un reino/facción/gremio/aldea con el que un jugador puede ganar reputación. */
public record Faction(String id, String displayName) implements RPGContent {

    public Faction {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

}
