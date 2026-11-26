package com.sack.rpgroll.common.structure;

import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

/** Bloque de una estructura leída: posición relativa a la esquina mínima; {@code tile} solo en bloques-entidad. */
public record SourceBlock(int x, int y, int z, BlockData data, BlockState tile) {
}
