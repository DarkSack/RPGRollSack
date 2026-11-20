package com.sack.rpgroll.emotes.catalog;

/**
 * Música de un emote: empieza al lanzarlo y se corta cuando termina.
 *
 * @param sound         clave del sonido (minúsculas), p. ej. {@code rpgroll_emotes:musica.baile1}
 * @param volume        volumen, de 0 a 4
 * @param lengthSeconds duración de la pista; con {@code loop} se repite cada tanto. 0 = no se repite
 */
public record EmoteMusic(String sound, float volume, double lengthSeconds) {
}
