package com.sack.rpgroll.structures.catalog;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/** Restricciones de colocación; listas o conjuntos vacíos = sin restricción. */
public record PlacementRules(boolean needsGround, boolean allowLiquids, List<String> worlds, List<String> biomes,
                             Set<Material> replace) {
}
