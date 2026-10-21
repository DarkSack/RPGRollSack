package com.sack.rpgroll.disasters.core;

/** Los fenómenos que sabe simular el motor. Un {@link Disaster} junta uno o varios. */
public enum DisasterType {
    /** Embudo que avanza hacia los jugadores, arrastra lo que pilla y lo lanza por los aires. */
    TORNADO,
    /** Meteoritos que caen cerca de los jugadores, estallan y dejan un cráter con un núcleo de menas. */
    METEOR_STORM,
    /** Tormenta que daña a quien está a cielo abierto, corroe el suelo, cubre de nieve o congela. */
    ACID_RAIN,
    /** Sacudidas, grietas que se abren en el suelo y rocas que caen del techo en cuevas y casas. */
    EARTHQUAKE,
    /** El suelo se hunde en un pozo redondo cerca de un jugador. */
    SINKHOLE,
    /** Aparecen mobs alrededor de los jugadores mientras dura (vanilla o de RPGRoll-Mobs). */
    MOBS;

    /** Los que pasan a cielo abierto: no tienen sentido en el Nether ni en el End. */
    public boolean needsSky() {
        return this != EARTHQUAKE && this != MOBS;
    }
}
