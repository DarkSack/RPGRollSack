package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.api.Capability;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.terrain.TerrainSpec;

import io.papermc.paper.math.Position;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.structure.Structure;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The built-in providers. New ones only need to implement {@link GenerationProvider}. */
public final class Providers {

    private final Map<String, GenerationProvider> byId = new LinkedHashMap<>();

    /** Without saved templates or terrains (tests, the validator's capability list). */
    public Providers() {
        this(name -> Result.fail("structure", "no hay plantillas en este contexto."), id -> Optional.empty());
    }

    /**
     * @param templates loads a saved template by name, for generators whose profile uses templates
     * @param terrains  finds a terrain of {@code terrains/} by id, for the "terrain" provider
     */
    public Providers(Function<String, Result<Structure>> templates, Function<String, Optional<TerrainSpec>> terrains) {
        register(new Simple("vanilla", "Minecraft sin cambios (opcional: amplificado o biomas grandes).",
                EnumSet.of(Capability.SEED, Capability.ENVIRONMENT, Capability.WORLD_TYPE, Capability.STRUCTURES,
                        Capability.SPAWN, Capability.PLATFORM), null, templates, terrains));
        register(new Simple("empty", "Sin terreno: el mundo está vacío salvo la plataforma y las plantillas que pida el perfil.",
                EnumSet.of(Capability.SEED, Capability.ENVIRONMENT, Capability.SPAWN, Capability.PLATFORM,
                        Capability.BIOMES, Capability.TEMPLATES), WorldForgeGenerator.Mode.EMPTY, templates, terrains));
        register(new Simple("flat", "Capas planas definidas por el perfil, con menas y plantillas propias si se piden.",
                EnumSet.of(Capability.SEED, Capability.ENVIRONMENT, Capability.SPAWN, Capability.PLATFORM,
                        Capability.LAYERS, Capability.BIOMES, Capability.ORES, Capability.TEMPLATES),
                WorldForgeGenerator.Mode.FLAT, templates, terrains));
        register(new Simple("hybrid", "Terreno vanilla más menas y plantillas propias; biomas existentes a elegir.",
                EnumSet.of(Capability.SEED, Capability.ENVIRONMENT, Capability.STRUCTURES, Capability.SPAWN,
                        Capability.PLATFORM, Capability.BIOMES, Capability.ORES, Capability.TEMPLATES),
                WorldForgeGenerator.Mode.HYBRID, templates, terrains));
        register(new Simple("terrain", "Terreno propio de WorldForge con los mismos ajustes que terrains/, para mundos "
                + "creados en caliente; superficie, cuevas, árboles y estructuras de Minecraft encima.",
                EnumSet.of(Capability.SEED, Capability.STRUCTURES, Capability.SPAWN, Capability.PLATFORM, Capability.ORES,
                        Capability.TEMPLATES), WorldForgeGenerator.Mode.TERRAIN, templates, terrains));
        register(new External());
    }

    public void register(GenerationProvider p) {
        byId.put(p.id(), p);
    }

    public Optional<GenerationProvider> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Map<String, GenerationProvider> all() {
        return Map.copyOf(byId);
    }

    /** id → capabilities, for the validator. */
    public Map<String, Set<Capability>> capabilities() {
        return byId.values().stream().collect(Collectors.toMap(GenerationProvider::id, GenerationProvider::capabilities));
    }

    public static World.Environment environment(String env) {
        return switch (env) {
            case "nether" -> World.Environment.NETHER;
            case "the_end" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
    }

    /**
     * Another plugin's generator (Terra, Iris…), asked for the same way bukkit.yml or Multiverse's
     * {@code -g} do. WorldForge only promises what it applies itself around it: seed, environment,
     * spawn and the platform built after creation. A plugin that gives no generator is an error,
     * never a silent fall back to vanilla.
     */
    private record External() implements GenerationProvider {

        @Override
        public String id() {
            return "plugin";
        }

        @Override
        public String description() {
            return "El generador de otro plugin (Terra, Iris…), como generator: en bukkit.yml o -g en Multiverse.";
        }

        @Override
        public Set<Capability> capabilities() {
            return EnumSet.of(Capability.SEED, Capability.ENVIRONMENT, Capability.SPAWN, Capability.PLATFORM);
        }

        @Override
        public ChunkGenerator generator(String world, Profile profile) {
            ChunkGenerator g = WorldCreator.getGeneratorForName(world, profile.generator(), Bukkit.getConsoleSender());
            if (g == null) {
                throw new IllegalStateException("el plugin no dio un generador para «" + profile.generator() + "» (la consola dice por qué).");
            }
            return g;
        }

        @Override
        public WorldCreator configure(WorldCreator c, Profile p) {
            c.environment(environment(p.environment()));
            if (p.seed() != null) {
                c.seed(p.seed());
            }
            c.generator(generator(c.name(), p));
            if (p.spawn() != null) {
                c.forcedSpawnPosition(Position.block(p.spawn().x(), p.spawn().y(), p.spawn().z()), 0f, 0f);
            }
            return c;
        }
    }

    /** The built-in providers: Minecraft's generator, or WorldForge's in one of its modes. */
    private record Simple(String id, String description, Set<Capability> capabilities, WorldForgeGenerator.Mode mode,
                          Function<String, Result<Structure>> templates, Function<String, Optional<TerrainSpec>> terrains)
            implements GenerationProvider {

        @Override
        public ChunkGenerator generator(String world, Profile profile) {
            if (mode == WorldForgeGenerator.Mode.TERRAIN) {
                TerrainSpec t = terrains.apply(profile.terrain()).orElseThrow(() -> new IllegalStateException(
                        "no hay un terreno «" + profile.terrain() + "» en terrains/."));
                return new WorldForgeGenerator(profile, mode, templates, t);
            }
            return mode == null ? null : new WorldForgeGenerator(profile, mode, templates);
        }

        @Override
        public WorldCreator configure(WorldCreator c, Profile p) {
            c.environment(environment(p.environment()));
            if (p.seed() != null) {
                c.seed(p.seed());
            }
            if (mode == null) {
                c.type(switch (p.worldType()) {
                    case "amplified" -> WorldType.AMPLIFIED;
                    case "large_biomes" -> WorldType.LARGE_BIOMES;
                    default -> WorldType.NORMAL;
                });
                c.generateStructures(!Boolean.FALSE.equals(p.structures()));
            } else {
                c.generator(generator(c.name(), p));
                c.generateStructures((mode == WorldForgeGenerator.Mode.HYBRID || mode == WorldForgeGenerator.Mode.TERRAIN)
                        && !Boolean.FALSE.equals(p.structures()));
            }
            if (p.spawn() != null) {
                c.forcedSpawnPosition(Position.block(p.spawn().x(), p.spawn().y(), p.spawn().z()), 0f, 0f);
            }
            return c;
        }
    }
}
