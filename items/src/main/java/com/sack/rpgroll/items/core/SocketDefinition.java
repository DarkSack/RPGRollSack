package com.sack.rpgroll.items.core;

import java.util.List;

/**
 * Una ranura de socket disponible en el ítem (ej. "socket-1"), que solo
 * acepta gemas cuyo {@code type} esté en {@code acceptedTypes}. Lista
 * vacía = acepta cualquier tipo de gema.
 */
public record SocketDefinition(String id, List<String> acceptedTypes) {

    public SocketDefinition {
        acceptedTypes = acceptedTypes == null ? List.of() : List.copyOf(acceptedTypes);
    }

    public boolean accepts(String gemType) {
        return acceptedTypes.isEmpty() || acceptedTypes.stream().anyMatch(t -> t.equalsIgnoreCase(gemType));
    }

}
