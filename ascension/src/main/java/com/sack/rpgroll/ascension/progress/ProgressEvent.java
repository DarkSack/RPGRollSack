package com.sack.rpgroll.ascension.progress;

/**
 * Algo que acaba de hacer un jugador y que puede contar para un logro o dar
 * reputación.
 *
 * @param target qué (tipo de entidad, material, id de quest o de mob, bioma)
 * @param held   material del ítem en la mano principal, o {@code null}
 */
public record ProgressEvent(TriggerType type, String target, String held) {

    public ProgressEvent {
        if (!type.isCounter()) {
            throw new IllegalArgumentException(type + " es un criterio de estado, no un evento");
        }
    }

}
