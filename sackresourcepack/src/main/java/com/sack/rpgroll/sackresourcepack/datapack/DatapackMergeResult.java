package com.sack.rpgroll.sackresourcepack.datapack;

import java.io.File;
import java.util.Map;

/**
 * @param fileOwners ruta relativa dentro de data/ (ej. "minecraft/tags/items/logs.json") -> id del módulo que la aportó en último lugar
 */
public record DatapackMergeResult(File outputDirectory, Map<String, String> fileOwners) {
}
