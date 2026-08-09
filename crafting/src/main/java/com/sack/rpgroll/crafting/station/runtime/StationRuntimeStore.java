package com.sack.rpgroll.crafting.station.runtime;

import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Persiste cada {@link StationRuntime} activo en su propio archivo plano
 * (mismo patrón "una entidad, un YAML" que el resto del ecosistema) bajo
 * {@code station-instances/<world>_<x>_<y>_<z>.yml}, para sobrevivir un
 * reinicio del servidor sin perder progreso ni contenido del inventario.
 */
public class StationRuntimeStore {

    private final File folder;
    private final CustomStationManager stationManager;
    private final Logger logger;

    public StationRuntimeStore(File dataFolder, CustomStationManager stationManager, Logger logger) {
        this.folder = new File(dataFolder, "station-instances");
        this.stationManager = stationManager;
        this.logger = logger;
    }

    public void save(StationRuntime runtime) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("station-id", runtime.stationDefId());
        config.set("world", runtime.world());
        config.set("x", runtime.x());
        config.set("y", runtime.y());
        config.set("z", runtime.z());
        config.set("active-recipe-id", runtime.activeRecipeId());
        config.set("progress-ticks", runtime.progressTicks());
        config.set("fuel-ticks-remaining", runtime.fuelTicksRemaining());
        config.set("last-player-id", runtime.lastPlayerId() != null ? runtime.lastPlayerId().toString() : null);

        ItemStack[] contents = runtime.inventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                config.set("inventory." + i, contents[i]);
            }
        }

        try {
            folder.mkdirs();
            config.save(new File(folder, fileName(runtime.world(), runtime.x(), runtime.y(), runtime.z())));
        } catch (IOException e) {
            logger.warning("✘ No se pudo guardar la estación en " + runtime.key() + ": " + e.getMessage());
        }
    }

    public void delete(String world, int x, int y, int z) {
        new File(folder, fileName(world, x, y, z)).delete();
    }

    public List<StationRuntime> loadAll() {

        List<StationRuntime> runtimes = new ArrayList<>();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return runtimes;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String stationDefId = config.getString("station-id");
            String world = config.getString("world");

            if (stationDefId == null || world == null) {
                continue;
            }

            Optional<CustomStation> def = stationManager.get(stationDefId);
            if (def.isEmpty()) {
                logger.warning("✘ Estación instanciada en '" + world + "' apunta a un station-id inexistente ('"
                        + stationDefId + "'), se ignora: " + file.getName());
                continue;
            }

            if (Bukkit.getWorld(world) == null) {
                continue;
            }

            int x = config.getInt("x");
            int y = config.getInt("y");
            int z = config.getInt("z");

            StationRuntime runtime = new StationRuntime(stationDefId, world, x, y, z,
                    def.get().inventorySize(), def.get().guiTitle());

            runtime.setLastPlayerId(config.getString("last-player-id") != null
                    ? UUID.fromString(config.getString("last-player-id")) : null);

            String activeRecipeId = config.getString("active-recipe-id");
            if (activeRecipeId != null) {
                runtime.startRecipe(activeRecipeId);
                runtime.setProgressTicks(config.getInt("progress-ticks", 0));
            }

            runtime.addFuelTicks(config.getInt("fuel-ticks-remaining", 0));

            var inventorySection = config.getConfigurationSection("inventory");
            if (inventorySection != null) {
                for (String slotKey : inventorySection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotKey);
                        ItemStack item = config.getItemStack("inventory." + slotKey);
                        if (slot >= 0 && slot < runtime.inventory().getSize()) {
                            runtime.inventory().setItem(slot, item);
                        }
                    } catch (NumberFormatException ignored) {
                        // clave de slot corrupta — se ignora ese ítem puntual
                    }
                }
            }

            runtimes.add(runtime);
        }

        return runtimes;
    }

    private String fileName(String world, int x, int y, int z) {
        return world + "_" + x + "_" + y + "_" + z + ".yml";
    }

}
