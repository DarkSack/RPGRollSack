package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.catalog.EmoteMusic;
import org.bukkit.entity.Player;

/** Costura de audio del servicio: así los tests no necesitan un mundo ni un planificador. */
public interface EmoteAudio {

    /** Tarea programada que se puede cancelar. */
    interface Cancellable {
        void cancel();
    }

    /** Empieza la pista desde el jugador (sigue al emisor). La oyen los cercanos según su volumen de discos. */
    void playMusic(Player p, EmoteMusic m);

    /**
     * Corta la pista a los jugadores del mismo mundo a 64 bloques o menos de {@code p}, incluido él.
     * <p>
     * Límite conocido: si dos jugadores cercanos usan la misma pista y uno corta, los oyentes dejan de
     * oír las dos hasta la siguiente repetición.
     */
    void stopMusic(Player p, EmoteMusic m);

    /** Ejecuta {@code r} dentro de {@code ticks} ticks en el hilo principal. */
    Cancellable later(Runnable r, long ticks);
}
