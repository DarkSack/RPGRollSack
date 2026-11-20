package com.sack.rpgroll.emotes.service;

import org.bukkit.Location;

/**
 * El emote que está haciendo un jugador.
 *
 * @param start dónde empezó, para cortarlo si se mueve
 * @param magic true = lo lanzó un hechizo, no el jugador
 */
public record EmoteSession(String emoteId, boolean loop, Location start, long startMillis, boolean magic) { }
