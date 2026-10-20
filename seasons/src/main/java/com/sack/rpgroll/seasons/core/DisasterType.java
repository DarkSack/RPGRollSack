package com.sack.rpgroll.seasons.core;

/** Los fenómenos que sabe simular el motor. Un {@link Disaster} junta uno o varios. */
public enum DisasterType {
    /** Embudo que avanza hacia los jugadores, arrastra lo que pilla y lo lanza por los aires. */
    TORNADO,
    /** Meteoritos que caen cerca de los jugadores y estallan sin romper bloques. */
    METEOR_STORM,
    /** Tormenta que quema a quien está a cielo abierto; un casco lo protege, pero se corroe. */
    ACID_RAIN,
    /** Sacudidas, grietas (solo visuales) y rocas que caen del techo en cuevas y casas. */
    EARTHQUAKE;

    /** Los que pasan a cielo abierto: no tienen sentido en el Nether ni en el End. */
    public boolean needsSky() {
        return this != EARTHQUAKE;
    }
}
