package com.sack.rpgroll.sackresourcepack.merge;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @param fileOwners ruta relativa dentro de assets/ (ej. "rpgroll_fishing/textures/item/salmon.png") -> id del módulo que la aportó en último lugar
 * @param overrides  pisadas detectadas (mismo path, contenido distinto) — no son necesariamente errores, pero sí para revisar
 */
public record MergeResult(File outputDirectory, Map<String, String> fileOwners, List<OverrideNotice> overrides) {
}
