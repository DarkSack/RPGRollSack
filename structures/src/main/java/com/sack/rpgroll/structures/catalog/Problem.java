package com.sack.rpgroll.structures.catalog;

/** Fallo de validación de un archivo de estructura; {@code key} es la clave del YAML afectada. */
public record Problem(String file, String key, String reason) {
}
