package com.sack.rpgroll.ranching.core.genetics;

/** Las dos copias que un animal tiene de UN gen — no hay noción de A/a discreta, cada alelo es un número. */
public record AllelePair(double alleleA, double alleleB) {

    public double resolve(GeneDominance dominance) {
        return dominance.resolve(alleleA, alleleB);
    }

}
