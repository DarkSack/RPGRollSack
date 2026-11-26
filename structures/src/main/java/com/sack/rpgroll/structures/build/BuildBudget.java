package com.sack.rpgroll.structures.build;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reparto del presupuesto de bloques por tick entre las obras activas.
 * Guarda estado (la última obra servida): se usa solo desde el hilo principal, sin sincronización.
 */
public final class BuildBudget {

    private final int perTick;
    private UUID lastServed;

    public BuildBudget(int perTick) {
        this.perTick = Math.max(0, perTick);
    }

    /**
     * Reparte {@code perTick} de uno en uno por turnos, empezando por la obra siguiente a la última servida,
     * para que una obra grande no deje sin turno a las demás. Nunca da más de lo pedido ni más de {@code perTick}.
     */
    public Map<UUID, Integer> share(List<UUID> ready, Map<UUID, Integer> wanted) {
        Map<UUID, Integer> given = new LinkedHashMap<>();
        int n = ready.size();
        if (n == 0) {
            return given;
        }
        int start = lastServed == null ? 0 : (ready.indexOf(lastServed) + 1) % n; // indexOf -1 (no está) -> 0
        int left = perTick;
        boolean progress = true;
        while (left > 0 && progress) {
            progress = false;
            for (int i = 0; i < n && left > 0; i++) {
                UUID id = ready.get((start + i) % n);
                if (given.getOrDefault(id, 0) < wanted.getOrDefault(id, 0)) {
                    given.merge(id, 1, Integer::sum);
                    lastServed = id;
                    left--;
                    progress = true;
                }
            }
        }
        return given;
    }
}
