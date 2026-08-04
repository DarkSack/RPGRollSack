package com.sack.rpgroll.ranching.core.genetics;

import java.util.UUID;

/**
 * Una referencia congelada a un ancestro — se guarda una copia (id, nombre,
 * especie) en el momento del nacimiento, en vez de tener que resolver el
 * {@code Animal} real más tarde, que puede ya no existir (murió, fue
 * vendido, el servidor purgó datos viejos). Así el linaje se puede mostrar
 * completo incluso mucho después de que un ancestro desapareció.
 */
public record AncestorRef(UUID id, String displayName, String speciesId) {
}
