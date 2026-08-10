package com.sack.rpgroll.extras.hud;

/**
 * Una línea del HUD (sección 25/26): {@code format} puede usar
 * {@code {value}}/{@code {max}}/{@code {bar}} — {@code {bar}} solo se
 * resuelve si {@code bar} es true, generando una barra ASCII de
 * {@code barLength} caracteres proporcional a value/max.
 */
public record HudLineFormat(String statId, String format, boolean bar, int barLength, char filledChar, char emptyChar) {
}
