package com.sack.rpgroll.tab.tablist;

/**
 * Una línea de cabecera o pie. {@code condition} (opcional) es la misma comparación que usan las líneas del
 * scoreboard ({@code "%rpgroll_max_level% == no"}); {@code block} (opcional) agrupa líneas que el jugador puede
 * mostrar u ocultar desde {@code /tab}.
 */
public record TablistLine(String text, String condition, String block) {
}
