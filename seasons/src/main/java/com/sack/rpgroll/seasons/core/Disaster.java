package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Un desastre natural de {@code disasters/}: uno o varios fenómenos a la vez durante
 * {@code durationTicks}. Sale solo (sorteo diario por mundo) o con {@code /seasonsadmin disaster}.
 *
 * @param intensity   multiplica la fuerza de cada fenómeno (daño, empuje, frecuencia); 1 = la de sus parámetros
 * @param dailyChance probabilidad 0-1 de que ocurra en un día de Minecraft en un mundo; 0 = solo por comando
 * @param seasonIds   estaciones en las que puede salir solo; vacío = cualquiera
 * @param worlds      mundos en los que puede salir solo; vacío = todos los que tienen estaciones
 * @param combine     ids de otros desastres cuyos fenómenos se suman a estos
 * @param messages    textos propios por clave ({@code start}, {@code title}, {@code subtitle}, {@code end});
 *                    la que falte sale del archivo de idioma, y {@code ""} la quita
 * @param startSound  sonido para cada jugador al empezar ({@code NOMBRE:volumen:tono}); {@code ""} = ninguno
 * @param startCommands comandos de consola al empezar ({@code %world%}, {@code %disaster%})
 * @param endCommands   comandos de consola al terminar
 */
public record Disaster(String id, String displayName, String icon, String description, int durationTicks,
        double intensity, double dailyChance, List<String> seasonIds, List<String> worlds, List<String> combine,
        List<DisasterPhenomenon> phenomena, Map<String, String> messages, String startSound,
        List<String> startCommands, List<String> endCommands) implements RPGContent {

    public Disaster {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "TNT" : icon;
        description = description == null ? "" : description;
        durationTicks = Math.max(20, durationTicks);
        intensity = intensity > 0 ? intensity : 1.0;
        dailyChance = Math.clamp(dailyChance, 0.0, 1.0);
        seasonIds = seasonIds == null ? List.of() : List.copyOf(seasonIds);
        worlds = worlds == null ? List.of() : List.copyOf(worlds);
        combine = combine == null ? List.of() : List.copyOf(combine);
        phenomena = phenomena == null ? List.of() : List.copyOf(phenomena);
        messages = messages == null ? Map.of() : Map.copyOf(messages);
        startSound = startSound == null ? "" : startSound;
        startCommands = startCommands == null ? List.of() : List.copyOf(startCommands);
        endCommands = endCommands == null ? List.of() : List.copyOf(endCommands);
    }

    /** Sus fenómenos más los de los desastres que combina (y los que combinen esos), cada desastre una vez. */
    public List<DisasterPhenomenon> allPhenomena(Function<String, Optional<Disaster>> lookup) {
        List<DisasterPhenomenon> all = new ArrayList<>();
        collect(this, lookup, new HashSet<>(), all);
        return all;
    }

    private static void collect(Disaster disaster, Function<String, Optional<Disaster>> lookup, Set<String> seen,
            List<DisasterPhenomenon> into) {
        if (!seen.add(disaster.id())) {
            return;
        }
        into.addAll(disaster.phenomena());
        for (String other : disaster.combine()) {
            lookup.apply(other).ifPresent(found -> collect(found, lookup, seen, into));
        }
    }

    /** Si el sorteo diario puede elegirlo en esta estación (null = sin estación) y este mundo. */
    public boolean canRollIn(String seasonId, String worldName) {
        return dailyChance > 0
                && (seasonIds.isEmpty() || (seasonId != null && seasonIds.contains(seasonId)))
                && (worlds.isEmpty() || worlds.contains(worldName));
    }

}
