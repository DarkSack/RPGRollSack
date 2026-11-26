package com.sack.rpgroll.structures.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Catálogo cargado; se reemplaza entero al recargar (los lectores nunca ven una mezcla). */
public final class StructureRegistry {

    private record State(Map<String, StructureDefinition> byId, List<Problem> problems) {}

    private volatile State state = new State(Map.of(), List.of());

    public void replace(StructureLoader.LoadResult result) {
        state = new State(result.valid().stream()
                .collect(Collectors.toUnmodifiableMap(StructureDefinition::id, Function.identity())),
                List.copyOf(result.problems()));
        RotatedCache.clear(); // las listas giradas del catálogo anterior ya no se piden
    }

    public Optional<StructureDefinition> get(String id) {
        return Optional.ofNullable(state.byId().get(id));
    }

    /** Por nivel y luego por nombre en español. */
    public List<StructureDefinition> inCategory(String cat) {
        return state.byId().values().stream().filter(s -> s.category().equalsIgnoreCase(cat))
                .sorted(Comparator.<StructureDefinition>comparingInt(s -> s.tier().level())
                        .thenComparing(s -> s.name("es"), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StructureDefinition::id))
                .toList();
    }

    /** Las que declaran {@code upgrade_from: <id>}, por nivel y luego por nombre en español. */
    public List<StructureDefinition> upgradesOf(String id) {
        return state.byId().values().stream().filter(s -> id.equals(s.upgradeFrom()))
                .sorted(Comparator.<StructureDefinition>comparingInt(s -> s.tier().level())
                        .thenComparing(s -> s.name("es"), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StructureDefinition::id))
                .toList();
    }

    public List<Problem> problems() {
        return state.problems();
    }
}
