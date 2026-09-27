package com.sack.rpgroll.furniture.core;

/** Sonidos al colocar y al retirar (claves de Minecraft, p. ej. {@code block.wood.place}). */
public record FurnitureSounds(String place, String breakSound) {

    public static final FurnitureSounds WOOD = new FurnitureSounds("block.wood.place", "block.wood.break");
}
