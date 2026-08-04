package com.sack.rpgroll.sackresourcepack.manifest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Orden topológico por {@code depends}/{@code optional}, con {@code
 * priority} (y luego el id) como desempate entre módulos sin relación de
 * dependencia entre sí — algoritmo de Kahn, determinista.
 */
public class DependencyResolver {

    public ResolutionResult resolve(List<AssetModule> modules) {

        List<String> errors = new ArrayList<>();
        Map<String, AssetModule> byId = new LinkedHashMap<>();

        for (AssetModule module : modules) {
            byId.put(module.id(), module);
        }

        // successors.get(x) = módulos que necesitan que x se procese antes.
        Map<String, Set<String>> successors = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new LinkedHashMap<>();

        for (AssetModule module : modules) {
            successors.putIfAbsent(module.id(), new TreeSet<>());
            inDegree.putIfAbsent(module.id(), 0);
        }

        for (AssetModule module : modules) {

            for (String dependencyId : module.depends()) {

                if (!byId.containsKey(dependencyId)) {
                    errors.add("Módulo '" + module.id() + "' depende de '" + dependencyId
                            + "', que no existe — se ignora esa dependencia.");
                    continue;
                }

                if (successors.get(dependencyId).add(module.id())) {
                    inDegree.merge(module.id(), 1, Integer::sum);
                }
            }

            for (String optionalId : module.optional()) {

                if (!byId.containsKey(optionalId)) {
                    continue;
                }

                if (successors.get(optionalId).add(module.id())) {
                    inDegree.merge(module.id(), 1, Integer::sum);
                }
            }
        }

        List<AssetModule> ordered = new ArrayList<>();
        Set<String> remaining = new TreeSet<>(byId.keySet());

        while (!remaining.isEmpty()) {

            String next = remaining.stream()
                    .filter(id -> inDegree.getOrDefault(id, 0) <= 0)
                    .min(Comparator.comparingInt((String id) -> byId.get(id).priority()).thenComparing(id -> id))
                    .orElse(null);

            if (next == null) {
                // Ciclo — no hay ningún módulo restante con in-degree 0. Se
                // agrega el resto en orden de prioridad/id, mejor esfuerzo,
                // y se reporta el problema en vez de trabar el pipeline.
                errors.add("Dependencia circular detectada entre: " + String.join(", ", remaining));

                remaining.stream()
                        .sorted(Comparator.comparingInt((String id) -> byId.get(id).priority()).thenComparing(id -> id))
                        .forEach(id -> ordered.add(byId.get(id)));

                remaining.clear();
                break;
            }

            ordered.add(byId.get(next));
            remaining.remove(next);

            for (String successorId : successors.get(next)) {
                inDegree.merge(successorId, -1, Integer::sum);
            }
        }

        return new ResolutionResult(ordered, errors);
    }

}
