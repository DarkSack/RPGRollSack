package com.sack.rpgroll.structures.api;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo funcional de una estructura terminada ({@code module.type} en su YAML). Todo se llama en el hilo principal;
 * un fallo de un módulo se apunta en el log y no para a los demás. {@code config} es la sección {@code module:} del
 * YAML de la estructura.
 */
public interface StructureModule {

    /** Valor de {@code module.type} en el YAML. */
    String type();

    /** Problemas de la sección {@code module:}; sus claves son relativas a {@code module.} (p. ej. {@code note.es}). */
    List<Problem> validate(ConfigurationSection config);

    /**
     * Como {@link #validate(ConfigurationSection)} pero con el esquemático ya leído (null si no se pudo leer); los
     * módulos que miran posiciones del esquemático lo sobrescriben.
     */
    default List<Problem> validate(ConfigurationSection config, BlockSource blocks) {
        return validate(config);
    }

    /** Al confirmar la obra y al arrancarla: una clave de idioma si no se puede construir ahí. */
    default Optional<String> precheck(UUID owner, String world, Box box, ConfigurationSection config) {
        return Optional.empty();
    }

    /** Una vez, cuando la base de datos ya registró la construcción terminada. */
    void onBuilt(BuiltStructure b);

    /** Cada cuántos ticks se llama {@link #onTick}; 0 = nunca. */
    default int tickEvery() {
        return 0;
    }

    /** Solo con el chunk del origen de la construcción cargado. */
    default void onTick(BuiltStructure b) {
    }

    /** Su registro ya se borró; los bloques siguen en el mundo. */
    void onRemoved(BuiltStructure b);
}
