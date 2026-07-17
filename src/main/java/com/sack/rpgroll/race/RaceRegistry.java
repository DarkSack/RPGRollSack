package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.race.loader.RaceLoader;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mantiene en memoria todas las razas cargadas desde plugins/RPGRoll/races/.
 * <p>
 * Responsable de:
 * - invocar RaceLoader para obtener las razas parseadas
 * - detectar y descartar ids duplicados
 * - exponer acceso de solo lectura al resto del framework
 * <p>
 * No sabe parsear YAML (RaceLoader) ni construir GUIs — solo almacena y expone.
 */
public class RaceRegistry {

    private final RPGRoll plugin;
    private final RaceLoader raceLoader;

    private final Map<String, Race> races = new LinkedHashMap<>();

    public RaceRegistry(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.raceLoader = new RaceLoader(plugin, yamlLoader);
    }

    /**
     * Carga (o recarga) todas las razas desde disco.
     * Descarta ids duplicados, quedándose con la primera ocurrencia
     * y avisando con un warning.
     */
    public void load() {

        races.clear();

        List<Race> loaded = raceLoader.load();

        for (Race race : loaded) {

            if (races.containsKey(race.id())) {
                plugin.getLogger().warning(
                        "✘ Raza duplicada ignorada: '" + race.id() + "' (ya existe una raza con ese id).");
                continue;
            }

            races.put(race.id(), race);
        }

        plugin.getLogger().info("✔ RaceRegistry: " + races.size() + " raza(s) activa(s).");
    }

    /**
     * Busca una raza por su id.
     *
     * @param id identificador de la raza (ej. "elfo")
     * @return la raza si existe
     */
    public Optional<Race> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(races.get(id));
    }

    /**
     * @return true si existe una raza registrada con ese id
     */
    public boolean exists(String id) {
        return id != null && races.containsKey(id);
    }

    /**
     * @return todas las razas cargadas, en orden de carga
     */
    public Collection<Race> getAll() {
        return List.copyOf(races.values());
    }

    /**
     * @return cantidad de razas actualmente registradas
     */
    public int count() {
        return races.size();
    }

}