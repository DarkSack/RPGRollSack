package com.sack.rpgroll.items.rarity;

import com.sack.rpgroll.common.content.RPGContent;

import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Objects;

/**
 * Rareza configurable (no un enum cerrado) — cada una define color, brillo,
 * sonido y partícula propios. El motor trae 9 registradas por defecto
 * (common..celestial, ver resources/rarities/) pero un servidor puede
 * agregar/editar las suyas libremente.
 */
public record Rarity(String id, String displayName, TextColor color, boolean glow, Sound sound, Particle particle)
        implements RPGContent {

    public Rarity {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(color, "color no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

}
