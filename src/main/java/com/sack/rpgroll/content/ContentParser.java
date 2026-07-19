package com.sack.rpgroll.content;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Convierte un YamlConfiguration en una instancia de contenido tipado.
 * Cada tipo de contenido (Race, PlayerClass, Job, Enchantment...) implementa
 * su propia versión — es el único lugar donde vive lógica específica de
 * parsing.
 *
 * @param <T> tipo de contenido que produce este parser
 */
@FunctionalInterface
public interface ContentParser<T extends RPGContent> {

    /**
     * @throws Exception si el YAML no se puede convertir en un T válido
     *                   (campo obligatorio faltante, valor inválido, etc.)
     */
    T parse(YamlConfiguration config) throws Exception;
}