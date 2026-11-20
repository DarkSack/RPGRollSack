package com.sack.rpgroll.emotes.catalog;

import java.util.Set;

/**
 * Un emote ya leído de su {@code .yml}.
 *
 * @param id          id normalizado (minúsculas)
 * @param name        MiniMessage, o clave de lang si empieza por {@code lang:}
 * @param icon        material o cabeza por textura
 * @param category    categoría normalizada (minúsculas)
 * @param animation   nombre de la animación dentro del modelo
 * @param model       modelo de BetterModel que la contiene
 * @param loop        true = sigue hasta un corte u otro emote
 * @param locked      true = hace falta desbloquearlo o tener el permiso
 * @param rarity      rareza normalizada (minúsculas), para color y orden
 * @param sound       sonido opcional (null si no hay)
 * @param particles   partículas opcionales en la cabeza (null si no hay)
 * @param cancel      motivos que cortan el emote
 * @param hidden      true = no sale en el menú hasta tenerlo
 * @param howToUnlock texto de cómo conseguirlo (null si no se indica)
 * @param music       música opcional, aparte del sonido corto (null si no hay)
 */
public record Emote(String id, String name, String icon, String category, String animation, String model,
                    boolean loop, boolean locked, String rarity, EmoteSound sound, EmoteParticles particles,
                    Set<CancelReason> cancel, boolean hidden, String howToUnlock, EmoteMusic music) {

    public Emote {
        cancel = Set.copyOf(cancel);
    }
}
