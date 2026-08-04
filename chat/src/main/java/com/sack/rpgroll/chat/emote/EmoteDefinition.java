package com.sack.rpgroll.chat.emote;

import com.sack.rpgroll.common.content.RPGContent;

/**
 * Emote de texto (spec: /wave, /laugh, /sit, /cry, /dance). {@code template}
 * usa {player} (emisor) y {target} (objetivo opcional, ej. "/wave Jugador").
 * Se anuncia a todos los jugadores dentro de {@code radius} bloques
 * (0 o negativo = todo el mundo actual).
 */
public record EmoteDefinition(String id, String template, String targetTemplate, double radius)
        implements RPGContent {

    public EmoteDefinition {
        template = template == null || template.isBlank() ? "{player} hace una acción." : template;
    }

}
