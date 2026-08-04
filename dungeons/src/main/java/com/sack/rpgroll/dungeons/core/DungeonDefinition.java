package com.sack.rpgroll.dungeons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Define una mazmorra completa: metadata, dónde vive físicamente
 * (lobby + bounds), su secuencia de salas, sus modos de dificultad,
 * política de revivir/checkpoints y loot final. Solo hay una corrida
 * activa por dungeon a la vez (ver filosofía de instanciación en
 * {@code DungeonEngine}) — no hay clonado de estructura.
 */
public record DungeonDefinition(
        String id,
        String category,
        String displayName,
        String icon,
        String description,
        int recommendedLevel,
        int estimatedMinutes,
        int minPlayers,
        int maxPlayers,
        long cooldownMillis,
        boolean repeatable,
        List<String> tags,
        DungeonPoint lobbyPoint,
        DungeonBounds bounds,
        List<DungeonRoom> rooms,
        List<DungeonDifficulty> difficulties,
        DungeonCheckpointPolicy checkpointPolicy,
        DungeonReviveConfig reviveConfig,
        List<DungeonLootEntry> loot,
        Map<DungeonTrigger, List<DungeonAction>> triggers) implements RPGContent {

    public DungeonDefinition {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        category = category == null || category.isBlank() ? "misc" : category;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "STONE_BRICKS" : icon.toUpperCase(java.util.Locale.ROOT);
        description = description == null ? "" : description;
        recommendedLevel = Math.max(1, recommendedLevel);
        estimatedMinutes = Math.max(1, estimatedMinutes);
        minPlayers = Math.max(1, minPlayers);
        maxPlayers = Math.max(minPlayers, maxPlayers);
        tags = tags == null ? List.of() : List.copyOf(tags);
        lobbyPoint = lobbyPoint == null ? new DungeonPoint("world", 0, 64, 0, 0, 0) : lobbyPoint;
        bounds = bounds == null ? DungeonBounds.none() : bounds;
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
        difficulties = difficulties == null || difficulties.isEmpty()
                ? List.of(DungeonDifficulty.defaultNormal()) : List.copyOf(difficulties);
        checkpointPolicy = checkpointPolicy == null ? DungeonCheckpointPolicy.defaultPolicy() : checkpointPolicy;
        reviveConfig = reviveConfig == null ? DungeonReviveConfig.none() : reviveConfig;
        loot = loot == null ? List.of() : List.copyOf(loot);
        triggers = triggers == null ? Map.of() : Map.copyOf(triggers);
    }

    public List<DungeonAction> actionsFor(DungeonTrigger trigger) {
        return triggers.getOrDefault(trigger, List.of());
    }

    public Optional<DungeonRoom> room(String roomId) {
        return rooms.stream().filter(room -> room.id().equalsIgnoreCase(roomId)).findFirst();
    }

    public Optional<DungeonRoom> roomAt(int index) {
        return index >= 0 && index < rooms.size() ? Optional.of(rooms.get(index)) : Optional.empty();
    }

    public int indexOfRoom(String roomId) {

        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).id().equalsIgnoreCase(roomId)) {
                return i;
            }
        }

        return -1;
    }

    public boolean isLastRoom(int index) {
        return index >= rooms.size() - 1;
    }

    public Optional<DungeonDifficulty> difficulty(String difficultyId) {
        return difficulties.stream().filter(d -> d.id().equalsIgnoreCase(difficultyId)).findFirst();
    }

}
