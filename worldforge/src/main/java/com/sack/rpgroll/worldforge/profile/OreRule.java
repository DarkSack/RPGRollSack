package com.sack.rpgroll.worldforge.profile;

import java.util.List;

/**
 * One ore rule, applied by a populator to chunks as they are generated (never to chunks
 * that already exist). {@code replaces} lists the blocks a vein may overwrite: a vein never
 * eats air, water or a block the rule does not name.
 *
 * @param veinsPerChunk attempts per chunk
 * @param size          blocks per vein (upper bound; fewer where nothing replaceable is near)
 * @param distribution  "uniform" or "triangle" (peak halfway between min-y and max-y)
 * @param priority      lower runs first
 */
public record OreRule(
        String id,
        String block,
        List<String> replaces,
        int veinsPerChunk,
        int size,
        int minY,
        int maxY,
        String distribution,
        List<String> biomes,
        List<String> excludeBiomes,
        int priority,
        boolean enabled) {

    public static final List<String> DEFAULT_REPLACES = List.of(
            "minecraft:stone", "minecraft:deepslate", "minecraft:granite", "minecraft:diorite",
            "minecraft:andesite", "minecraft:tuff");

    public OreRule {
        replaces = List.copyOf(replaces);
        biomes = List.copyOf(biomes);
        excludeBiomes = List.copyOf(excludeBiomes);
    }
}
