package com.sack.rpgroll.ranching.core.genetics;

/** Estadísticas de una simulación Monte Carlo de un gen para el Breeding Planner (modo PROBABILISTIC). */
public record GenePreviewStats(String geneId, String geneDisplayName, double min, double average, double max) {
}
