package com.sack.rpgroll.player.jobs;

import java.util.Set;

/**
 * Progreso específico del trabajo Explorador: biomas visitados y contador
 * de distancia recorrida desde el último pago. Se guarda como una extensión
 * de JobProgress (mismo level/experience), más este estado adicional.
 */
public record ExplorerProgress(Set<String> visitedBiomes, double distanceSinceLastPayout) {

    public ExplorerProgress {
        visitedBiomes = visitedBiomes == null ? Set.of() : Set.copyOf(visitedBiomes);
        distanceSinceLastPayout = Math.max(0, distanceSinceLastPayout);
    }

    public static ExplorerProgress empty() {
        return new ExplorerProgress(Set.of(), 0.0);
    }

    public boolean hasVisited(String biome) {
        return visitedBiomes.contains(biome);
    }

    public ExplorerProgress withNewBiome(String biome) {
        var updated = new java.util.HashSet<>(visitedBiomes);
        updated.add(biome);
        return new ExplorerProgress(updated, distanceSinceLastPayout);
    }

    public ExplorerProgress withDistance(double distance) {
        return new ExplorerProgress(visitedBiomes, distance);
    }

    public ExplorerProgress resetDistance() {
        return new ExplorerProgress(visitedBiomes, 0.0);
    }

}