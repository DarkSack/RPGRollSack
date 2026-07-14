package com.sack.rpgroll.config;

/**
 * Representa un archivo de configuración administrado por el framework.
 *
 * @param resource    Ruta dentro del JAR (src/main/resources)
 * @param destination Ruta relativa dentro de plugins/RPGRoll/
 * @param required    Indica si el archivo es obligatorio
 */
public record ConfigFile(
                String resource,
                String destination,
                boolean required) {
}