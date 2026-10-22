package com.sack.rpgroll.worldforge;

import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.generation.Providers;
import com.sack.rpgroll.worldforge.profile.Catalog;
import com.sack.rpgroll.worldforge.profile.ProfileValidator;

import java.util.Map;
import java.util.Set;

/** A fixed "server" for tests: the blocks and biomes the built-in presets use, and a few more. */
public final class TestCatalog implements Catalog {

    public static final Set<String> BLOCKS = Set.of("minecraft:stone", "minecraft:deepslate", "minecraft:granite",
            "minecraft:diorite", "minecraft:andesite", "minecraft:tuff", "minecraft:dirt", "minecraft:grass_block",
            "minecraft:bedrock", "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:deepslate_gold_ore",
            "minecraft:deepslate_diamond_ore", "minecraft:diamond_ore", "minecraft:glass");
    public static final Set<String> BIOMES = Set.of("minecraft:plains", "minecraft:the_void", "minecraft:desert",
            "minecraft:forest");

    @Override
    public boolean isBlock(String key) {
        return BLOCKS.contains(key);
    }

    @Override
    public boolean isBiome(String key) {
        return BIOMES.contains(key);
    }

    /** Saved templates of this fake server: name → {x, y, z}. */
    public static final Map<String, int[]> TEMPLATES = Map.of("torre", new int[] {5, 9, 5}, "sala", new int[] {7, 5, 9},
            "enorme", new int[] {40, 10, 8}, "alta", new int[] {8, 70, 8});

    @Override
    public Result<int[]> template(String name) {
        int[] size = TEMPLATES.get(name);
        return size == null ? Result.fail("structure", "no existe la plantilla «" + name + "».") : Result.ok(size);
    }

    /** Plugins "installed" on this fake server that give generators. */
    public static final Set<String> GENERATOR_PLUGINS = Set.of("OtroGenerador", "Terra");

    @Override
    public String generatorProblem(String spec) {
        String name = spec.split(":", 2)[0];
        return GENERATOR_PLUGINS.contains(name) ? null : "no hay ningún plugin «" + name + "» instalado.";
    }

    /** Terrains of this fake server. */
    public static final Set<String> TERRAINS = Set.of("montanas", "suave");

    @Override
    public boolean isTerrain(String id) {
        return TERRAINS.contains(id);
    }

    public static ProfileValidator validator() {
        return new ProfileValidator(new Providers().capabilities(), new TestCatalog());
    }
}
