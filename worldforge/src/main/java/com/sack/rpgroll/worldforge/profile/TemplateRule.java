package com.sack.rpgroll.worldforge.profile;

import java.util.List;

/**
 * One template rule: a saved structure ({@code structures/<structure>.nbt}) that a populator
 * places in some of the chunks Minecraft generates from now on (never in existing ones).
 *
 * @param rarity    one try every {@code rarity} chunks on average (1 = every chunk)
 * @param placement "surface": on the first solid block from the top (never on water), raised by
 *                  {@code yOffset}, only if that ground is between min-y and max-y;
 *                  "underground": the bottom of the template at a random height in [min-y, max-y]
 * @param minY      null = the bottom of the world
 * @param maxY      null = the top of the world
 * @param rotation  "random" (one of the four) or "none"
 */
public record TemplateRule(
        String id,
        String structure,
        int rarity,
        String placement,
        Integer minY,
        Integer maxY,
        int yOffset,
        String rotation,
        List<String> biomes,
        List<String> excludeBiomes,
        boolean enabled) {

    public TemplateRule {
        biomes = List.copyOf(biomes);
        excludeBiomes = List.copyOf(excludeBiomes);
    }
}
