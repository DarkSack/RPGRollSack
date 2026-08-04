package com.sack.rpgroll.sackresourcepack.manifest;

import java.util.List;

/**
 * Orden final de build (menor índice = se fusiona primero, los
 * siguientes pueden pisarlo) más cualquier problema encontrado — nunca
 * lanza excepción por una dependencia faltante o un ciclo: los reporta
 * acá y sigue con el mejor orden posible, para que un solo módulo mal
 * configurado no tumbe todo el pipeline.
 */
public record ResolutionResult(List<AssetModule> orderedModules, List<String> errors) {
}
