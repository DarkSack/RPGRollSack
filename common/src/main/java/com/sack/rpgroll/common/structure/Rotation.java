package com.sack.rpgroll.common.structure;

import org.bukkit.block.structure.StructureRotation;

/** Giro en planta (mirando desde arriba), mismo sentido que {@link StructureRotation}. */
public enum Rotation {
    NONE(StructureRotation.NONE),
    CW_90(StructureRotation.CLOCKWISE_90),
    CW_180(StructureRotation.CLOCKWISE_180),
    CCW_90(StructureRotation.COUNTERCLOCKWISE_90);

    private final StructureRotation bukkit;

    Rotation(StructureRotation bukkit) {
        this.bukkit = bukkit;
    }

    public Rotation next() {
        Rotation[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public StructureRotation toBukkit() {
        return bukkit;
    }

    private boolean swaps() {
        return this == CW_90 || this == CCW_90;
    }

    public int rotatedWidth(int w, int l) {
        return swaps() ? l : w;
    }

    public int rotatedLength(int w, int l) {
        return swaps() ? w : l;
    }

    /** {x', z'} ya desplazado para quedar en 0..w'-1, 0..l'-1. */
    public int[] apply(int x, int z, int width, int length) {
        return switch (this) {
            case NONE -> new int[] { x, z };
            case CW_90 -> new int[] { length - 1 - z, x };
            case CW_180 -> new int[] { width - 1 - x, length - 1 - z };
            case CCW_90 -> new int[] { z, width - 1 - x };
        };
    }
}
