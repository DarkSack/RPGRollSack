package com.sack.rpgroll.structures.preview;

import java.util.List;

/** Resultado de {@link TerrainCheck}: choques {x,y,z} y clave de lang del primer fallo (null = se puede). */
public record CheckResult(List<int[]> collisions, String reason) {

    public boolean ok() {
        return reason == null;
    }
}
