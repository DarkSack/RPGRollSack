/**
 * API pública de RPGRoll para addons externos.
 * <p>
 * Punto de entrada único: {@link com.sack.rpgroll.api.RPGRollAPI#get()}.
 * <p>
 * Contrato de estabilidad: las clases de este paquete (y sus subpaquetes,
 * como {@code api.event}) forman la superficie pública soportada del
 * framework. Cambios que rompan compatibilidad binaria en este paquete
 * requieren un bump de versión mayor. El resto de los paquetes de RPGRoll
 * (todo fuera de {@code com.sack.rpgroll.api}) son detalles de
 * implementación internos y pueden cambiar sin previo aviso entre
 * versiones — los addons nunca deben importar clases fuera de este
 * paquete y de {@code api.event}.
 */
package com.sack.rpgroll.api;