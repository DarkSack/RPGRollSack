package com.sack.rpgroll.workers.core.schedule;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Un horario diario — lista de {@link ScheduleEntry} que se repite todos
 * los días de Minecraft. Compatible con RPGRoll-Seasons en el sentido de
 * que el reloj que consulta ({@code World#getTime()}) es el mismo que
 * usa Seasons para sus propios días — no hay una integración más
 * profunda que esa (un horario no cambia de un día a otro por estación).
 */
public record Schedule(String id, String displayName, String description, List<ScheduleEntry> entries)
        implements RPGContent {

    public Schedule {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /** La actividad vigente a una hora dada del día (0-24000) — la última entrada cuyo startTick ya pasó. */
    public ScheduleActivity activityAt(long tick) {

        if (entries.isEmpty()) {
            return ScheduleActivity.FREE;
        }

        long normalized = ((tick % 24000) + 24000) % 24000;

        List<ScheduleEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong(ScheduleEntry::startTick));

        ScheduleActivity current = sorted.get(sorted.size() - 1).activity();

        for (ScheduleEntry entry : sorted) {

            if (entry.startTick() > normalized) {
                break;
            }

            current = entry.activity();
        }

        return current;
    }

}
