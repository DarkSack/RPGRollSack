package com.sack.rpgroll.race;

import com.sack.rpgroll.RPGRoll;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Almacén en memoria de las razas activas.
 * <p>
 * Responsabilidad única: mantener el registro de {@link Race} ya cargadas
 * y exponer acceso de solo lectura. No sabe leer YAML, no invoca RaceLoader,
 * no decide cuándo recargar — eso es responsabilidad de RaceManager.
 */
public class RaceRegistry {

    private final RPGRoll plugin;
    private final Map<String, Race> races = new LinkedHashMap<>();

    public RaceRegistry(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra una raza. Si ya existe una con el mismo id, la ignora
     * y avisa con un warning, quedándose con la primera registrada.
     *
     * @return true si se registró, false si era un id duplicado
     */
    public boolean register(Race race) {

        if (races.containsKey(race.id())) {
            plugin.getLogger().warning(
                    "✘ Raza duplicada ignorada: '" + race.id() + "' (ya existe una raza con ese id).");
            return false;
        }

        races.put(race.id(), race);
        return true;
    }

    /**
     * Elimina todas las razas registradas. Usado antes de una recarga completa.
     */
    public void clear() {
        races.clear();
    }

    public Optional<Race> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(races.get(id));
    }

    public boolean exists(String id) {
        return id != null && races.containsKey(id);
    }

    public Collection<Race> getAll() {
        return List.copyOf(races.values());
    }

    public int count() {
        return races.size();
    }

}