package com.sack.rpgroll.crates.core;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

/**
 * Una posible recompensa dentro de un {@link Crate}. La probabilidad de
 * que salga sorteada es {@code weight} dividido por la suma de weights
 * de todas las recompensas del mismo crate (ver {@link CrateRewardSelector}).
 */
public record CrateReward(
        String id,
        String displayName,
        Material icon,
        List<String> lore,
        double weight,
        boolean announceGlobally,
        List<CrateAction> actions) {

    public CrateReward {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");
        Objects.requireNonNull(icon, "icon no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (weight <= 0) {
            throw new IllegalArgumentException("weight debe ser mayor a 0 (reward '" + id + "')");
        }

        lore = lore == null ? List.of() : List.copyOf(lore);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

}
