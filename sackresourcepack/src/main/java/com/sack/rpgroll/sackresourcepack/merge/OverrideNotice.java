package com.sack.rpgroll.sackresourcepack.merge;

/** Un módulo posterior pisó un archivo de otro con contenido distinto en la misma ruta — la Validation Engine lo reporta como warning. */
public record OverrideNotice(String relativePath, String previousModuleId, String newModuleId) {
}
