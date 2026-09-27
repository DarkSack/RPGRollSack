package com.sack.rpgroll.furniture.placed;

import com.sack.rpgroll.furniture.placed.BlockKey.ChunkKey;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Qué mueble ocupa cada casilla, solo de los chunks cargados.
 * <p>
 * Se reconstruye desde las entidades: al arrancar con lo ya cargado y después con cada
 * {@code EntitiesLoadEvent} (no {@code ChunkLoadEvent}: en Paper las entidades cargan aparte y
 * en ese momento aún pueden no estar). Nada de esto se guarda en disco.
 */
public class FurnitureIndex {

    /** Lo que hace falta de un mueble sin buscar su entidad (partículas, límites). */
    public record Entry(UUID display, String furnitureId, BlockKey anchor, float yaw, int state) {
    }

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Map<BlockKey, UUID> occupied = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Set<UUID>> byChunk = new ConcurrentHashMap<>();
    private final Map<UUID, List<BlockKey>> cellsOf = new ConcurrentHashMap<>();

    public void add(PlacedFurniture furniture) {

        remove(furniture.uuid());

        BlockKey anchor = BlockKey.of(furniture.anchor());
        Entry entry = new Entry(furniture.uuid(), furniture.furnitureId(), anchor, furniture.yaw(), furniture.state());
        entries.put(entry.display(), entry);

        List<BlockKey> cells = new java.util.ArrayList<>(furniture.barriers());
        if (!cells.contains(anchor)) {
            cells.add(anchor);
        }
        cells.forEach(cell -> occupied.put(cell, entry.display()));
        cellsOf.put(entry.display(), List.copyOf(cells));
        byChunk.computeIfAbsent(anchor.chunk(), k -> ConcurrentHashMap.newKeySet()).add(entry.display());
    }

    public void remove(UUID display) {

        Entry entry = entries.remove(display);
        List<BlockKey> cells = cellsOf.remove(display);

        if (cells != null) {
            cells.forEach(cell -> occupied.remove(cell, display));
        }
        if (entry != null) {
            Set<UUID> inChunk = byChunk.get(entry.anchor().chunk());
            if (inChunk != null) {
                inChunk.remove(display);
                if (inChunk.isEmpty()) {
                    byChunk.remove(entry.anchor().chunk(), inChunk);
                }
            }
        }
    }

    /** Actualiza el estado guardado en el índice (para las partículas que dependen de él). */
    public void updateState(UUID display, int state) {
        entries.computeIfPresent(display, (k, e) -> new Entry(e.display(), e.furnitureId(), e.anchor(), e.yaw(), state));
    }

    public boolean isOccupied(Block block) {
        return occupied.containsKey(BlockKey.of(block));
    }

    public Optional<UUID> displayAt(Block block) {
        return Optional.ofNullable(occupied.get(BlockKey.of(block)));
    }

    /** El mueble que ocupa esta casilla, si su entidad está cargada. */
    public Optional<ItemDisplay> entityAt(Block block) {
        return displayAt(block).map(Bukkit::getEntity).filter(e -> e instanceof ItemDisplay && e.isValid())
                .map(e -> (ItemDisplay) e);
    }

    public int countInChunk(ChunkKey chunk) {
        Set<UUID> set = byChunk.get(chunk);
        return set == null ? 0 : set.size();
    }

    public int countInChunk(ChunkKey chunk, String furnitureId) {

        Set<UUID> set = byChunk.get(chunk);
        if (set == null) {
            return 0;
        }
        return (int) set.stream().map(entries::get)
                .filter(e -> e != null && e.furnitureId().equals(furnitureId)).count();
    }

    public Collection<Entry> entries() {
        return entries.values();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        occupied.clear();
        byChunk.clear();
        cellsOf.clear();
    }
}
