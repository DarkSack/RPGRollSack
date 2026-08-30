package com.sack.rpgroll.dungeons.structure;

/**
 * De dónde sale la geometría de una {@link StructureDefinition} — sección
 * "estructuras físicas" de la Biblioteca. {@code NATIVE} referencia un
 * archivo .nbt real de Structure Block vanilla (construido a mano en
 * survival); {@code CUSTOM} es una paleta + capas de texto propias, el
 * único formato que puede generarse sin correr un servidor de Minecraft;
 * {@code SCHEMATIC} referencia un archivo .schem importado de WorldEdit
 * (requiere WorldEdit instalado para pegarse — ver {@code SchematicBridge}).
 */
public enum StructureSourceType {
    CUSTOM,
    NATIVE,
    SCHEMATIC
}
