package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Requisitos para desbloquear un traits/clase/raza "secreto" — el
 * contenido en sí (Race/PlayerClass/Trait) sigue viviendo en :core; esto
 * solo describe CUÁNDO se considera desbloqueado para un jugador. Cubre
 * "Rasgos Ocultos", "Clases Secretas" y "Razas Secretas" con un único
 * concepto, ya que los tres son "contenido de :core con gate adicional".
 * {@code id} es el id de esta entrada de desbloqueo, no del contenido —
 * {@code targetId} es el id real en :core.
 */
public record SecretUnlockRequirement(String id, SecretTargetType targetType, String targetId,
        AscensionRequirements requirements) implements RPGContent {

    public SecretUnlockRequirement {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(targetType, "targetType no puede ser null");
        Objects.requireNonNull(targetId, "targetId no puede ser null");
        requirements = requirements == null ? AscensionRequirements.none() : requirements;
    }

}
