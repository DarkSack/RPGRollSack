package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

/**
 * Define un TIPO de crate (ej. "comun", "legendario") — no una ubicación
 * física en el mundo, eso es {@link com.sack.rpgroll.crates.location.PlacedCrate}.
 * Varias ubicaciones pueden compartir el mismo Crate (mismas recompensas,
 * misma llave, mismo holograma).
 */
public record Crate(
        String id,
        String displayName,
        String guiTitle,
        boolean requireKey,
        Material keyMaterial,
        String keyDisplayName,
        List<String> keyLore,
        List<String> hologramLines,
        List<CrateReward> rewards) implements RPGContent {

    public Crate {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(displayName, "displayName no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("el crate '" + id + "' necesita al menos una recompensa");
        }

        guiTitle = guiTitle == null || guiTitle.isBlank() ? displayName : guiTitle;
        keyMaterial = keyMaterial == null ? Material.TRIPWIRE_HOOK : keyMaterial;
        keyDisplayName = keyDisplayName == null || keyDisplayName.isBlank()
                ? "Llave: " + displayName
                : keyDisplayName;
        keyLore = keyLore == null ? List.of() : List.copyOf(keyLore);
        hologramLines = hologramLines == null ? List.of() : List.copyOf(hologramLines);
        rewards = List.copyOf(rewards);
    }

}
