package com.sack.rpgroll.crafting.ingredient;

/**
 * Traduce el id de una {@code QualityDefinition} a un rango numérico comparable
 * (mayor = mejor). Inyectado desde fuera para que este paquete no dependa del
 * sistema de calidad (evita un ciclo de paquetes ingredient <-> quality).
 */
@FunctionalInterface
public interface QualityRankResolver {

    int rankOf(String qualityId);

    QualityRankResolver ALWAYS_ZERO = qualityId -> 0;
}
