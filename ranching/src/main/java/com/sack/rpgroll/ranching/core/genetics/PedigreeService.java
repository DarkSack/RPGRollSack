package com.sack.rpgroll.ranching.core.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Construye la lista de ancestros de una cría al nacer, y chequea
 * endogamia — todo a partir de listas ya congeladas de {@link AncestorRef},
 * nunca resolviendo animales reales, así que sigue funcionando aunque un
 * ancestro ya haya muerto o sido vendido.
 */
public class PedigreeService {

    private static final int MAX_STORED_GENERATIONS = 6;

    /** Arma la lista de ancestros de una cría a partir de sus dos padres — los padres primero, después SUS ancestros. */
    public List<AncestorRef> buildAncestry(AncestorRef mother, AncestorRef father, List<AncestorRef> motherAncestors,
            List<AncestorRef> fatherAncestors) {

        List<AncestorRef> result = new ArrayList<>();
        result.add(mother);
        result.add(father);

        int remaining = MAX_STORED_GENERATIONS * 2;

        for (AncestorRef ref : motherAncestors) {
            if (remaining-- <= 0) {
                break;
            }
            result.add(ref);
        }

        for (AncestorRef ref : fatherAncestors) {
            if (remaining-- <= 0) {
                break;
            }
            result.add(ref);
        }

        return result;
    }

    /**
     * true si los dos candidatos comparten un ancestro dentro de las últimas
     * {@code generations} generaciones (o si uno es directamente ancestro del otro).
     */
    public boolean isInbred(UUID motherId, List<AncestorRef> motherAncestors, UUID fatherId,
            List<AncestorRef> fatherAncestors, int generations) {

        if (motherId.equals(fatherId)) {
            return true;
        }

        List<UUID> motherLine = limitedIds(motherAncestors, generations);
        List<UUID> fatherLine = limitedIds(fatherAncestors, generations);

        if (motherLine.contains(fatherId) || fatherLine.contains(motherId)) {
            return true;
        }

        for (UUID id : motherLine) {
            if (fatherLine.contains(id)) {
                return true;
            }
        }

        return false;
    }

    private List<UUID> limitedIds(List<AncestorRef> ancestors, int generations) {

        int limit = Math.max(0, generations) * 2;
        List<UUID> result = new ArrayList<>();

        for (int i = 0; i < ancestors.size() && i < limit; i++) {
            result.add(ancestors.get(i).id());
        }

        return result;
    }

}
