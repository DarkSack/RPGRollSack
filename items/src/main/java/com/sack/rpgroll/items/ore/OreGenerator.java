package com.sack.rpgroll.items.ore;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Siembra las menas al cargar cada chunk.
 *
 * <p>En los chunks nuevos siempre. En los que ya existían, solo en los mundos
 * de {@code ores.retrogen-worlds} (pensado para el mundo de recursos: el
 * principal tiene construcciones y no se toca lo que ya está). Cada chunk
 * apunta en sus datos qué menas ya recibió, así que añadir una mena nueva
 * más adelante solo siembra esa.
 */
public class OreGenerator implements Listener {

    private final OreService service;
    private final NamespacedKey sownKey;
    private final Supplier<Set<String>> retrogenWorlds;

    public OreGenerator(Plugin plugin, OreService service, Supplier<Set<String>> retrogenWorlds) {
        this.service = service;
        this.sownKey = new NamespacedKey(plugin, "ores_sown");
        this.retrogenWorlds = retrogenWorlds;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {

        if (!service.hasOres()) {
            return;
        }

        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();

        if (!event.isNewChunk() && !retrogenWorlds.get().contains(world.getName())) {
            return;
        }

        Map<OreDefinition, List<OreDefinition.OreGeneration>> todo = new HashMap<>();
        for (OreDefinition ore : service.ores().getAll()) {
            for (OreDefinition.OreGeneration generation : ore.generation()) {
                if (generation.worlds().contains(world.getName())) {
                    todo.computeIfAbsent(ore, key -> new java.util.ArrayList<>()).add(generation);
                }
            }
        }

        if (todo.isEmpty()) {
            return;
        }

        var data = chunk.getPersistentDataContainer();
        String raw = data.get(sownKey, PersistentDataType.STRING);
        Set<String> sown = new LinkedHashSet<>(raw == null || raw.isBlank() ? List.of() : Arrays.asList(raw.split(",")));

        boolean changed = false;
        for (var entry : todo.entrySet()) {
            OreDefinition ore = entry.getKey();
            if (sown.contains(ore.id())) {
                continue;
            }
            int index = 0;
            for (OreDefinition.OreGeneration generation : entry.getValue()) {
                sow(chunk, ore, generation, index++);
            }
            sown.add(ore.id());
            changed = true;
        }

        if (changed) {
            data.set(sownKey, PersistentDataType.STRING, String.join(",", sown));
            service.countChunk();
        }
    }

    private void sow(Chunk chunk, OreDefinition ore, OreDefinition.OreGeneration generation, int index) {

        World world = chunk.getWorld();
        int minY = Math.max(generation.minY(), world.getMinHeight());
        int maxY = Math.min(generation.maxY(), world.getMaxHeight() - 1);
        if (maxY < minY) {
            return;
        }

        Map<String, BlockData> variants = new HashMap<>();
        for (var entry : ore.blocks().entrySet()) {
            try {
                variants.put(entry.getKey(), org.bukkit.Bukkit.createBlockData(entry.getValue()));
            } catch (IllegalArgumentException ignored) {
                // Ya avisado en OreService#rebuild.
            }
        }

        Random random = new Random(world.getSeed() ^ (chunk.getX() * 341873128712L) ^ (chunk.getZ() * 132897987541L)
                ^ ((long) ore.id().hashCode() << 8) ^ index);

        int blocks = 0;
        int veins = OreMath.veinCount(generation.veinsPerChunk(), random);

        for (int v = 0; v < veins; v++) {

            int size = generation.veinMin() + random.nextInt(generation.veinMax() - generation.veinMin() + 1);
            List<int[]> vein = OreMath.vein(random, random.nextInt(16), minY + random.nextInt(maxY - minY + 1),
                    random.nextInt(16), size, minY, maxY);

            for (int[] pos : vein) {
                Block block = chunk.getBlock(pos[0], pos[1], pos[2]);
                String variant = generation.replace().get(block.getType().name());
                BlockData state = variant == null ? null : variants.get(variant);
                if (state != null) {
                    block.setBlockData(state, false);
                    blocks++;
                }
            }
        }

        if (blocks > 0) {
            service.countPlaced(ore.id(), blocks);
        }
    }

}
