package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

public record NpcDefinition(
        String id,
        String displayName,
        String skinValue,
        String skinSignature,
        String pose,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        List<NpcAction> actions) implements RPGContent {

    public NpcDefinition {

        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        skinValue = skinValue == null ? "" : skinValue;
        skinSignature = skinSignature == null ? "" : skinSignature;

        pose = pose == null || pose.isBlank()
                ? "STANDING"
                : pose.toUpperCase();

        actions = actions == null
                ? List.of()
                : List.copyOf(actions);
    }

    public boolean hasCustomSkin() {
        return !skinValue.isBlank() && !skinSignature.isBlank();
    }
}