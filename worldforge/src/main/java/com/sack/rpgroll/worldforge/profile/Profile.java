package com.sack.rpgroll.worldforge.profile;

import java.util.List;

/**
 * A generation profile: what a world should look like. Immutable; editing makes a copy.
 * Field meaning and limits are in {@link ProfileValidator}; YAML shape in {@link ProfileCodec}.
 *
 * @param seed     null = random at creation
 * @param spawn    null = the provider decides
 * @param platform   null = no platform (nothing is built unless the profile asks)
 * @param structures null = the provider's default
 * @param generator  only with provider "plugin": another plugin's generator, "Plugin" or "Plugin:id"
 * @param playerGravity null = normal; else a factor of Minecraft's gravity for players in the world
 * @param terrain    only with provider "terrain": the id of a terrain in {@code terrains/}
 */
public record Profile(
        int schema,
        String id,
        String name,
        String description,
        String minecraft,
        String provider,
        Long seed,
        String environment,
        String worldType,
        Boolean structures,
        Spawn spawn,
        Platform platform,
        List<Layer> layers,
        List<String> biomes,
        List<OreRule> ores,
        List<TemplateRule> templates,
        Double playerGravity,
        String generator,
        String terrain) {

    public static final int SCHEMA = 1;
    public static final String TARGET_MINECRAFT = "26.1.2";

    public Profile {
        layers = List.copyOf(layers);
        biomes = List.copyOf(biomes);
        ores = List.copyOf(ores);
        templates = List.copyOf(templates);
    }

    public record Spawn(int x, int y, int z) {
    }

    /** A square of {@code block} with side {@code 2*radius+1}, centred on the spawn column, at height {@code y}. */
    public record Platform(String block, int radius, int y) {
    }

    public record Layer(String block, int height) {
    }

    public Profile withId(String newId, String newName) {
        return new Profile(schema, newId, newName, description, minecraft, provider, seed, environment, worldType,
                structures, spawn, platform, layers, biomes, ores, templates, playerGravity, generator, terrain);
    }

    public Profile withSeed(Long newSeed) {
        return new Profile(schema, id, name, description, minecraft, provider, newSeed, environment, worldType,
                structures, spawn, platform, layers, biomes, ores, templates, playerGravity, generator, terrain);
    }

    public Profile withProvider(String newProvider) {
        return new Profile(schema, id, name, description, minecraft, newProvider, seed, environment, worldType,
                structures, spawn, platform, layers, biomes, ores, templates, playerGravity, generator, terrain);
    }

    public Profile withStructures(Boolean on) {
        return new Profile(schema, id, name, description, minecraft, provider, seed, environment, worldType,
                on, spawn, platform, layers, biomes, ores, templates, playerGravity, generator, terrain);
    }

    public Profile withBiomes(List<String> newBiomes) {
        return new Profile(schema, id, name, description, minecraft, provider, seed, environment, worldType,
                structures, spawn, platform, layers, newBiomes, ores, templates, playerGravity, generator, terrain);
    }

    public Profile withOres(List<OreRule> newOres) {
        return new Profile(schema, id, name, description, minecraft, provider, seed, environment, worldType,
                structures, spawn, platform, layers, biomes, newOres, templates, playerGravity, generator, terrain);
    }
}
