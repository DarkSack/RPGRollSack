package com.sack.rpgroll.emotes.engine;

import org.bukkit.entity.Player;

/** Lo que el servicio necesita del motor de animación; en el servidor lo hace {@link BetterModelEngine}. */
public interface EmoteEngine {

    /**
     * Reproduce la animación en el jugador.
     *
     * @param onEnd se llama una sola vez, tanto si acaba sola como si la cierran por fuera, y siempre
     *              en el hilo principal del servidor
     * @return false si el modelo o la animación no existen o no se pudo arrancar
     */
    boolean play(Player player, String model, String animation, boolean loop, Runnable onEnd);

    /** Corta lo que esté reproduciendo el jugador (dispara su {@code onEnd}). */
    void stop(Player player);

    /** Duración en segundos, o -1 si no existe. */
    float lengthSeconds(String model, String animation);

    boolean exists(String model, String animation);
}
