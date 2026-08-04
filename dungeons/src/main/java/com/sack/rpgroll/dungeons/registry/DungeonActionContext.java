package com.sack.rpgroll.dungeons.registry;

import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonRoom;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Contexto disponible al ejecutar una {@link com.sack.rpgroll.dungeons.core.DungeonAction}.
 * Guarda solo el {@code sessionId} (no la sesión en sí) para que este
 * paquete no dependa de {@code engine} — el handler de{@code PROGRESS_OBJECTIVE}
 * (registrado por el motor, no acá) resuelve la sesión real por id.
 */
public record DungeonActionContext(
        DungeonDefinition definition,
        UUID sessionId,
        List<Player> players,
        Player targetPlayer,
        DungeonRoom room) {

    public Player anyPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }

}
