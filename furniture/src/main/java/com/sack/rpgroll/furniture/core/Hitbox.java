package com.sack.rpgroll.furniture.core;

import java.util.List;

/**
 * Lo que ocupa un mueble en el mundo y dónde se le hace clic.
 * <p>
 * {@code BARRIER}: bloques de barrera en {@code blocks}; se chocan con ellos y se puede poner
 * otro mueble encima (una lámpara sobre una mesa). {@code INTERACTION}: una entidad Interaction
 * de {@code width} por {@code height} en el bloque del mueble, que se atraviesa (lámparas de
 * pie, plantas, alfombras, cuadros).
 * <p>
 * Los desplazamientos de {@code blocks} son los del mueble mirando al sur; al colocarlo
 * mirando a otro lado se giran con él.
 */
public record Hitbox(Type type, List<Offset> blocks, float width, float height) {

    public enum Type {
        BARRIER,
        INTERACTION
    }

    public Hitbox {
        blocks = List.copyOf(blocks);
        if (type == Type.BARRIER && blocks.isEmpty()) {
            blocks = List.of(Offset.ZERO);
        }
        width = Math.max(0.05f, width);
        height = Math.max(0.05f, height);
    }

    public static Hitbox barrier(List<Offset> blocks) {
        return new Hitbox(Type.BARRIER, blocks, 1f, 1f);
    }

    public static Hitbox interaction(float width, float height) {
        return new Hitbox(Type.INTERACTION, List.of(), width, height);
    }
}
