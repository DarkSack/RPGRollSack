package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.genetics.BreedingOutcome;
import com.sack.rpgroll.ranching.core.species.Sex;

import java.util.List;
import java.util.UUID;

/**
 * Una cría ya concebida (genética resuelta) esperando a nacer — se congela
 * por completo en el momento de la concepción, así que el nacimiento no
 * depende de que el padre siga existiendo cuando termine la gestación.
 */
public record PendingOffspring(BreedingOutcome outcome, Sex sex, String breedId, int generation,
        List<AncestorRef> ancestry, UUID motherId, UUID fatherId) {
}
