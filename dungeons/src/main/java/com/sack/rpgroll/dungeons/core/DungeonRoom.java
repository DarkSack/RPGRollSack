package com.sack.rpgroll.dungeons.core;

import java.util.List;
import java.util.Map;

/**
 * Una habitación de la dungeon. Las salas corren en el orden en que
 * aparecen en {@code DungeonDefinition#rooms()} — no hay ramificación
 * (a diferencia de las quests): es una secuencia lineal de sala en sala.
 */
public record DungeonRoom(
        String id,
        DungeonRoomType type,
        DungeonPoint entryPoint,
        List<DungeonObjective> objectives,
        List<DungeonWave> waves,
        String bossMobId,
        Map<DungeonTrigger, List<DungeonAction>> events) {

    public DungeonRoom {
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        waves = waves == null ? List.of() : List.copyOf(waves);
        events = events == null ? Map.of() : Map.copyOf(events);
    }

    public List<DungeonAction> eventsFor(DungeonTrigger trigger) {
        return events.getOrDefault(trigger, List.of());
    }

    public boolean hasBoss() {
        return bossMobId != null && !bossMobId.isBlank();
    }

}
