package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

public record Junk(String id, String displayName, String icon, String description, double weight)
        implements RPGContent {

    public Junk {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "LEATHER_BOOTS" : icon;
        description = description == null ? "" : description;
        weight = Math.max(0.01, weight);
    }

}
