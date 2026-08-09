package com.sack.rpgroll.crafting.station.runtime;

import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Mantiene en memoria todas las {@link StationRuntime} activas del servidor, indexadas por ubicación. */
public class StationRuntimeRegistry {

    private final Map<String, StationRuntime> runtimes = new LinkedHashMap<>();
    private final Map<Inventory, StationRuntime> byInventory = new LinkedHashMap<>();
    private final StationRuntimeStore store;

    public StationRuntimeRegistry(StationRuntimeStore store) {
        this.store = store;
    }

    public void loadAll() {
        runtimes.clear();
        byInventory.clear();
        for (StationRuntime runtime : store.loadAll()) {
            runtimes.put(runtime.key(), runtime);
            byInventory.put(runtime.inventory(), runtime);
        }
    }

    public Optional<StationRuntime> findByInventory(Inventory inventory) {
        return Optional.ofNullable(byInventory.get(inventory));
    }

    public void saveAll() {
        for (StationRuntime runtime : runtimes.values()) {
            store.save(runtime);
        }
    }

    public StationRuntime getOrCreate(String stationDefId, String world, int x, int y, int z, int inventorySize,
            String guiTitle) {

        String key = StationRuntime.keyOf(world, x, y, z);
        StationRuntime runtime = runtimes.computeIfAbsent(key,
                k -> new StationRuntime(stationDefId, world, x, y, z, inventorySize, guiTitle));
        byInventory.putIfAbsent(runtime.inventory(), runtime);
        return runtime;
    }

    public Optional<StationRuntime> get(String world, int x, int y, int z) {
        return Optional.ofNullable(runtimes.get(StationRuntime.keyOf(world, x, y, z)));
    }

    public void remove(String world, int x, int y, int z) {
        StationRuntime removed = runtimes.remove(StationRuntime.keyOf(world, x, y, z));
        if (removed != null) {
            byInventory.remove(removed.inventory());
        }
        store.delete(world, x, y, z);
    }

    public Collection<StationRuntime> getAll() {
        return runtimes.values();
    }

}
