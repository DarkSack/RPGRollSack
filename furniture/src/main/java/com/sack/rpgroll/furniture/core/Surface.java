package com.sack.rpgroll.furniture.core;

import org.bukkit.block.BlockFace;

/** Dónde se puede apoyar un mueble: en el suelo, colgado de una pared o del techo. */
public enum Surface {
    FLOOR,
    WALL,
    CEILING;

    /** La superficie que corresponde a la cara del bloque sobre la que se hizo clic. */
    public static Surface of(BlockFace clickedFace) {
        return switch (clickedFace) {
            case UP -> FLOOR;
            case DOWN -> CEILING;
            default -> WALL;
        };
    }
}
