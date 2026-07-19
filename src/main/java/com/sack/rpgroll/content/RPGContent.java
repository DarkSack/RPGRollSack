package com.sack.rpgroll.content;

/**
 * Contrato mínimo que debe cumplir cualquier tipo de contenido cargado
 * desde YAML por el framework (razas, clases, trabajos, encantamientos, etc.).
 * <p>
 * Como todos son records con un componente "id", implementar esta interfaz
 * no requiere código adicional — el accessor generado por el record ya
 * satisface el contrato.
 */
public interface RPGContent {
    String id();
}