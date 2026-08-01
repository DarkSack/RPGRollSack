package com.sack.rpgroll.npcs.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Mantiene la sesión de edición activa por admin (uno a la vez). */
public class NpcSessionManager {

    private final Map<UUID, NpcEditSession> sessions = new HashMap<>();

    public void start(UUID admin, NpcEditSession session) {
        sessions.put(admin, session);
    }

    public Optional<NpcEditSession> get(UUID admin) {
        return Optional.ofNullable(sessions.get(admin));
    }

    public void end(UUID admin) {
        sessions.remove(admin);
    }

}