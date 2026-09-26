package com.sack.rpgroll.items.ore;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una mena propia: un estado de bloque vanilla que el resource pack dibuja
 * como otra cosa (normalmente un bloque musical con instrumento de cabeza,
 * que nadie puede crear a mano con {@code disable-noteblock-updates}).
 *
 * <p>{@code blocks} va de variante ({@code stone}, {@code deepslate},
 * {@code netherrack}...) a estado ({@code minecraft:note_block[instrument=zombie,note=3,powered=false]});
 * cada generación dice qué bloque natural se sustituye por qué variante.
 * Al picarla suelta {@code dropItem} (un ítem de RPGRoll-Items o
 * {@code gem:<id>}), si el pico llega a {@code requiredTier}.
 *
 * @param hardness dureza como la de vanilla (la de diamante es 3): fija cuánto se tarda en picarla
 */
public record OreDefinition(
        String id,
        String displayName,
        Map<String, String> blocks,
        String dropItem,
        int dropMin,
        int dropMax,
        boolean fortune,
        int xpMin,
        int xpMax,
        double hardness,
        int requiredTier,
        List<OreGeneration> generation) implements RPGContent {

    public OreDefinition {
        Objects.requireNonNull(id, "id");
        blocks = Map.copyOf(blocks);
        generation = generation == null ? List.of() : List.copyOf(generation);
        displayName = displayName == null ? id : displayName;
    }

    /**
     * Dónde y cuánto se siembra.
     *
     * @param replace material natural (STONE, DEEPSLATE, NETHERRACK...) → variante de {@code blocks}
     */
    public record OreGeneration(List<String> worlds, int minY, int maxY, double veinsPerChunk, int veinMin,
            int veinMax, Map<String, String> replace) {

        public OreGeneration {
            worlds = List.copyOf(worlds);
            replace = Map.copyOf(replace);
        }
    }

}
