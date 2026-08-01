package com.sack.rpgroll.common.resource;

/**
 * Representa un único archivo empaquetado en resources/ que debe copiarse
 * a la carpeta de datos del plugin la primera vez que arranca.
 *
 * @param resource    ruta dentro del JAR (relativa a src/main/resources)
 * @param destination ruta relativa dentro de la carpeta de datos del plugin
 * @param required    si es obligatorio (se loguea como error, no warning, cuando falta)
 */
public record ResourceFile(
        String resource,
        String destination,
        boolean required) {
}
