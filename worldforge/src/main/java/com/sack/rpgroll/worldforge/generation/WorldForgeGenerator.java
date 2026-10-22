package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.profile.OreRule;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.profile.TemplateRule;
import com.sack.rpgroll.worldforge.terrain.TerrainSpec;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.structure.Structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WorldForge's chunk generator, built once from a profile. Materials and biomes are
 * resolved here, so generation (which may run off the main thread) only reads immutable data.
 *
 * <ul>
 *   <li>EMPTY: no Minecraft phase runs; only the profile's platform, if any.</li>
 *   <li>FLAT: the profile's layers from the bottom of the world up.</li>
 *   <li>HYBRID: every Minecraft phase runs; WorldForge only adds populators (FEASIBILITY P3).</li>
 *   <li>TERRAIN: {@link TerrainEngine} shapes the land; Minecraft's surface, caves, decorations
 *       and structures run on top of it.</li>
 * </ul>
 */
public final class WorldForgeGenerator extends ChunkGenerator {

    public enum Mode { EMPTY, FLAT, HYBRID, TERRAIN }

    private final Mode mode;
    private final Profile profile;
    private final List<Material> layerBlocks = new ArrayList<>();
    private final List<Integer> layerHeights = new ArrayList<>();
    private final List<Biome> biomes;
    private final List<BlockPopulator> populators = new ArrayList<>();
    private final TemplatePopulator templates;
    private final Material platformBlock;
    private final TerrainSpec terrain;
    private final Map<String, Biome> terrainBiomes;
    private volatile TerrainEngine engine;

    /** @param templates loads a saved template by name (it logs why one cannot be used; that rule is then left out) */
    public WorldForgeGenerator(Profile profile, Mode mode, Function<String, Result<Structure>> templates) {
        this(profile, mode, templates, null);
    }

    /** @param terrain the shape for {@link Mode#TERRAIN}; null for the other modes */
    public WorldForgeGenerator(Profile profile, Mode mode, Function<String, Result<Structure>> templates, TerrainSpec terrain) {
        if (mode == Mode.TERRAIN && terrain == null) {
            throw new IllegalArgumentException("el modo TERRAIN necesita un terreno");
        }
        this.profile = profile;
        this.mode = mode;
        this.terrain = terrain;
        this.terrainBiomes = mode != Mode.TERRAIN ? Map.of()
                : TerrainEngine.BIOMES.stream().collect(Collectors.toMap(k -> k, WorldForgeGenerator::biome, (a, b) -> a, HashMap::new));
        for (Profile.Layer l : profile.layers()) {
            layerBlocks.add(material(l.block()));
            layerHeights.add(l.height());
        }
        this.biomes = profile.biomes().stream().map(WorldForgeGenerator::biome).toList();
        List<OreRule> rules = profile.ores().stream().filter(OreRule::enabled)
                .sorted(Comparator.comparingInt(OreRule::priority)).toList();
        if (!rules.isEmpty()) {
            populators.add(new OrePopulator(rules));
        }
        List<TemplatePopulator.Rule> placed = new ArrayList<>();
        for (TemplateRule t : profile.templates()) {
            Result<Structure> s = t.enabled() ? templates.apply(t.structure()) : null;
            if (s != null && s.isOk()) {
                placed.add(new TemplatePopulator.Rule(t, s.value(), Set.copyOf(t.biomes()), Set.copyOf(t.excludeBiomes())));
            }
        }
        this.templates = placed.isEmpty() ? null : new TemplatePopulator(placed);
        if (this.templates != null) {
            populators.add(this.templates);
        }
        this.platformBlock = profile.platform() == null ? null : material(profile.platform().block());
    }

    public Mode mode() {
        return mode;
    }

    public Profile profile() {
        return profile;
    }

    /** The terrain of {@link Mode#TERRAIN}; null in the other modes. */
    public TerrainSpec terrain() {
        return terrain;
    }

    /** Template rule id → {placed, did not fit}, and the last placements ("id world x y z ROTATION"); null without templates. */
    public Map<String, long[]> templateCounts() {
        return templates == null ? null : templates.stats().counts();
    }

    public List<String> recentTemplates() {
        return templates == null ? List.of() : templates.stats().recent();
    }

    static Material material(String key) {
        Material m = Material.matchMaterial(key);
        if (m == null || !m.isBlock()) {
            throw new IllegalArgumentException("«" + key + "» no es un bloque (el validador debió rechazarlo)");
        }
        return m;
    }

    static Biome biome(String key) {
        NamespacedKey k = NamespacedKey.fromString(key);
        Biome b = k == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(k);
        if (b == null) {
            throw new IllegalArgumentException("«" + key + "» no es un bioma (el validador debió rechazarlo)");
        }
        return b;
    }

    /** The engine for this world's seed and height, built on first use. */
    private TerrainEngine engine(WorldInfo info) {
        TerrainEngine e = engine;
        if (e == null || e.seed() != info.getSeed()) {
            synchronized (this) {
                e = engine;
                if (e == null || e.seed() != info.getSeed()) {
                    engine = e = new TerrainEngine(terrain, info.getSeed(), info.getMinHeight(), info.getMaxHeight());
                }
            }
        }
        return e;
    }

    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData data) {
        if (mode == Mode.TERRAIN) {
            TerrainEngine e = engine(info);
            int sea = e.seaLevel(), bottom = data.getMinHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int h = e.height((chunkX << 4) + x, (chunkZ << 4) + z);
                    data.setRegion(x, bottom, z, x + 1, h + 1, z + 1, Material.STONE);
                    if (h < sea) {
                        data.setRegion(x, h + 1, z, x + 1, sea + 1, z + 1, Material.WATER);
                    }
                }
            }
        }
        if (mode == Mode.FLAT) {
            int y = data.getMinHeight();
            for (int i = 0; i < layerBlocks.size() && y < data.getMaxHeight(); i++) {
                int top = Math.min(data.getMaxHeight(), y + layerHeights.get(i));
                data.setRegion(0, y, 0, 16, top, 16, layerBlocks.get(i));
                y = top;
            }
        }
        // The platform is part of generation, so it also exists in a world set in bukkit.yml.
        if (platformBlock != null) {
            int cx = profile.spawn() == null ? 0 : profile.spawn().x();
            int cz = profile.spawn() == null ? 0 : profile.spawn().z();
            int r = profile.platform().radius(), y = profile.platform().y(), bx = chunkX << 4, bz = chunkZ << 4;
            int x0 = Math.max(cx - r, bx), x1 = Math.min(cx + r, bx + 15);
            int z0 = Math.max(cz - r, bz), z1 = Math.min(cz + r, bz + 15);
            if (x0 <= x1 && z0 <= z1) {
                data.setRegion(x0 - bx, y, z0 - bz, x1 - bx + 1, y + 1, z1 - bz + 1, platformBlock);
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return mode == Mode.HYBRID;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return mode == Mode.HYBRID || mode == Mode.TERRAIN;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return mode == Mode.HYBRID || mode == Mode.TERRAIN;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return mode == Mode.HYBRID || mode == Mode.TERRAIN;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return mode == Mode.HYBRID || mode == Mode.TERRAIN;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return (mode == Mode.HYBRID || mode == Mode.TERRAIN) && !Boolean.FALSE.equals(profile.structures());
    }

    /** Where structures and spawn look for the ground: the engine's surface (water counts where the heightmap says so). */
    @Override
    public int getBaseHeight(WorldInfo info, Random random, int x, int z, HeightMap heightMap) {
        if (mode != Mode.TERRAIN) {
            return super.getBaseHeight(info, random, x, z, heightMap);
        }
        TerrainEngine e = engine(info);
        int h = e.height(x, z);
        boolean fluids = heightMap != HeightMap.OCEAN_FLOOR && heightMap != HeightMap.OCEAN_FLOOR_WG;
        return (fluids ? Math.max(h, e.seaLevel()) : h) + 1;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return List.copyOf(populators);
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo info) {
        if (mode == Mode.TERRAIN) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo w, int x, int y, int z) {
                    return terrainBiomes.get(engine(w).biome(x, y, z));
                }

                @Override
                public List<Biome> getBiomes(WorldInfo w) {
                    return List.copyOf(terrainBiomes.values());
                }
            };
        }
        return biomes.isEmpty() ? null : new CellBiomes(biomes);
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        if (profile.spawn() != null) {
            return new Location(world, profile.spawn().x() + 0.5, profile.spawn().y(), profile.spawn().z() + 0.5);
        }
        if (mode == Mode.FLAT) {
            int top = world.getMinHeight() + layerHeights.stream().mapToInt(Integer::intValue).sum();
            return new Location(world, 0.5, Math.min(top, world.getMaxHeight() - 1), 0.5);
        }
        if (mode == Mode.TERRAIN) {
            // The nearest dry land on a spiral of 16-block steps, so nobody starts in the sea.
            TerrainEngine e = engine(world);
            for (int r = 0; r <= 2048; r += 16) {
                for (int i = -r; i <= r; i += 16) {
                    for (int[] c : new int[][] {{i, -r}, {i, r}, {-r, i}, {r, i}}) {
                        int h = e.height(c[0], c[1]);
                        if (h > e.seaLevel() + 1) {
                            return new Location(world, c[0] + 0.5, h + 1, c[1] + 0.5);
                        }
                    }
                }
            }
            return new Location(world, 0.5, e.seaLevel() + 1, 0.5);
        }
        return mode == Mode.EMPTY ? new Location(world, 0.5, 64, 0.5) : null;
    }

    /**
     * Existing biomes in square cells of {@link #CELL} blocks, picked by a hash of the cell
     * and the seed: the same seed gives the same map. It only chooses among existing biomes;
     * it does not create any.
     */
    static final class CellBiomes extends BiomeProvider {

        static final int CELL = 256;
        private final List<Biome> biomes;

        CellBiomes(List<Biome> biomes) {
            this.biomes = List.copyOf(biomes);
        }

        @Override
        public Biome getBiome(WorldInfo info, int x, int y, int z) {
            return biomes.get(cell(info.getSeed(), Math.floorDiv(x, CELL), Math.floorDiv(z, CELL), biomes.size()));
        }

        @Override
        public List<Biome> getBiomes(WorldInfo info) {
            return biomes;
        }

        static int cell(long seed, int cx, int cz, int count) {
            long h = seed * 0x9E3779B97F4A7C15L + cx * 0xC2B2AE3D27D4EB4FL + cz * 0x165667B19E3779F9L;
            h ^= h >>> 31;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 29;
            return (int) Math.floorMod(h, (long) count);
        }
    }
}
