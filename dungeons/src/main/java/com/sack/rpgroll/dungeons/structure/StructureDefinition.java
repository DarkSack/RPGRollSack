package com.sack.rpgroll.dungeons.structure;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Una entrada de la Biblioteca de Estructuras: o bien datos propios
 * (paleta de caracteres + capas de texto, {@link StructureSourceType#CUSTOM})
 * o una referencia a un archivo {@code structures/<id>.nbt} de Structure
 * Block vanilla que vive junto a este YAML ({@link StructureSourceType#NATIVE}).
 * <p>
 * Para NATIVE, {@code width/height/depth/anchor/palette/layers} quedan en
 * sus valores por defecto — el tamaño real solo se conoce al cargar el .nbt
 * (ver {@link StructurePasteEngine}), y el anclaje de pegado ya viene
 * implícito en el propio archivo (la esquina donde estaba el Structure Block).
 */
public record StructureDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        StructureSourceType sourceType,
        int width,
        int height,
        int depth,
        /** Offset dentro de la grilla (x,y,z) que coincide con el punto de pegado — solo CUSTOM. */
        int anchorX,
        int anchorY,
        int anchorZ,
        Map<Character, Material> palette,
        /** {@code layers.get(y).get(z)} es una fila de texto, un carácter por bloque en X — piso primero (y=0). */
        List<List<String>> layers) implements RPGContent {
}
