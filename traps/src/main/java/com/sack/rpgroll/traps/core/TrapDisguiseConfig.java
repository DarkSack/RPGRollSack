package com.sack.rpgroll.traps.core;

import org.bukkit.Material;

/**
 * Camuflaje v1: swap de bloque REAL (visible igual para todos), no
 * per-jugador vía paquetes — eso requeriría ProtocolLib y quedó fuera de
 * alcance (ver plan). {@code visibleMaterial} es lo que ve todo el mundo
 * mientras la trampa está ARMED; {@code revealedMaterial} reemplaza el
 * bloque en el momento del disparo (TRIGGERED), delatando la trampa.
 */
public record TrapDisguiseConfig(DisguiseMode mode, Material visibleMaterial, Material revealedMaterial) {

    public enum DisguiseMode {
        NONE,
        BLOCK_SWAP
    }

    public static TrapDisguiseConfig none() {
        return new TrapDisguiseConfig(DisguiseMode.NONE, null, null);
    }

    public boolean isActive() {
        return mode == DisguiseMode.BLOCK_SWAP && visibleMaterial != null;
    }

}
