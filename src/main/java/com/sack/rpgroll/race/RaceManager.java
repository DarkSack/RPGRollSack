package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.race.loader.RaceLoader;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Servicio principal del sistema de razas.
 * <p>
 * Coordina el flujo completo: invoca {@link RaceLoader} para leer y parsear
 * los YAML desde disco, y registra cada raza válida en {@link RaceRegistry}.
 * Expone una API de solo lectura simple para el resto del framework.
 * <p>
 * No parsea YAML directamente (RaceLoader) ni almacena directamente
 * (RaceRegistry) — solo orquesta, mismo rol que ConfigManager cumple
 * sobre DirectoryCreator/ResourceCopier/YamlLoader.
 */
public class RaceManager {

    private final RPGRoll plugin;
    private final RaceLoader raceLoader;
    private final RaceRegistry raceRegistry;

    public RaceManager(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.raceLoader = new RaceLoader(plugin, yamlLoader);
        this.raceRegistry = new RaceRegistry(plugin);
    }

    /**
     * Inicializa el sistema de razas: carga desde disco vía RaceLoader
     * y registra cada raza válida en RaceRegistry. Descarta duplicados.
     * Seguro de llamar múltiples veces — cada llamada es una recarga completa.
     */
    public void initialize() {

        plugin.getLogger().info("Inicializando sistema de razas...");

        raceRegistry.clear();

        List<Race> loaded = raceLoader.load();

        int registered = 0;
        int duplicates = 0;

        for (Race race : loaded) {
            if (raceRegistry.register(race)) {
                registered++;
            } else {
                duplicates++;
            }
        }

        plugin.getLogger().info("✔ Razas leídas desde disco: " + loaded.size());
        plugin.getLogger().info("✔ Razas registradas: " + registered);

        if (duplicates > 0) {
            plugin.getLogger().warning("✘ Razas duplicadas ignoradas: " + duplicates);
        }

        if (registered == 0) {
            plugin.getLogger().warning(
                    "✘ No se registró ninguna raza. Los jugadores no podrán elegir raza al crear personaje.");
        }
    }

    /**
     * Vuelve a cargar todas las razas desde disco, descartando el estado anterior.
     * Pensado para invocarse desde /rpg reload.
     */
    public void reload() {
        initialize();
    }

    // ============ API pública de consulta ============

    public Optional<Race> get(String id) {
        return raceRegistry.get(id);
    }

    public boolean exists(String id) {
        return raceRegistry.exists(id);
    }

    public Collection<Race> getAll() {
        return raceRegistry.getAll();
    }

    public int count() {
        return raceRegistry.count();
    }

}