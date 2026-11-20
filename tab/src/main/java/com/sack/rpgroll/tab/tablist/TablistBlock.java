package com.sack.rpgroll.tab.tablist;

/**
 * Un grupo de líneas de cabecera/pie que cada jugador puede ocultar desde {@code /tab}.
 * {@code icon} es el material del botón en el menú (null = el de por defecto); el nombre sale del idioma
 * ({@code options.blocks.<id>}) o, si no está, de {@code name}.
 */
public record TablistBlock(String id, String name, String icon, boolean shownByDefault) {
}
