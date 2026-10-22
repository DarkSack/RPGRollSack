package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.api.Result;

/**
 * What exists on this server: blocks and biomes (datapack biomes included). The live one
 * reads the server registries; tests pass a fixed list.
 */
public interface Catalog {

    /** {@code key} is normalized ("minecraft:stone"). */
    boolean isBlock(String key);

    boolean isBiome(String key);

    /** Size {x, y, z} of the saved template {@code name} (an id already checked), or why it cannot be used. */
    Result<int[]> template(String name);

    /** Why another plugin's generator ("Plugin" or "Plugin:id", shape already checked) cannot be used; null if it can. */
    String generatorProblem(String spec);

    /** Whether {@code terrains/} has a terrain with this id. */
    boolean isTerrain(String id);
}
