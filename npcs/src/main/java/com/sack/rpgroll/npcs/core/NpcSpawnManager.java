package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.npcs.render.FakePlayerRenderer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asigna UUID/entityId fijos por NPC, y decide visibilidad por distancia:
 * a qué jugadores se les muestra cada NPC activo.
 * <p>
 * entityId empieza en un rango alto arbitrario para minimizar colisión con
 * IDs reales de entidades del mundo — no hay garantía absoluta de que nunca
 * choque, pero en la práctica es extremadamente improbable con este offset.
 */
public class NpcSpawnManager {

    private static final int ENTITY_ID_START = 900_000;
    private static final double VISIBILITY_RADIUS = 48.0;

    private final Plugin plugin;
    private final FakePlayerRenderer renderer;
    private final AtomicInteger nextEntityId = new AtomicInteger(ENTITY_ID_START);

    private final Map<String, UUID> npcUuids = new HashMap<>();
    private final Map<String, Integer> npcEntityIds = new HashMap<>();

    // Por jugador, qué npcIds tiene actualmente visibles
    private final Map<UUID, java.util.Set<String>> visibleTo = new HashMap<>();

    public NpcSpawnManager(Plugin plugin, FakePlayerRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
    }

    /**
     * Registra un NPC en el sistema (le asigna UUID/entityId si no los tenía).
     * No lo muestra a nadie todavía — eso lo maneja updateVisibility().
     */
    public void register(NpcDefinition npc) {
        npcUuids.computeIfAbsent(npc.id(), id -> UUID.randomUUID());
        npcEntityIds.computeIfAbsent(npc.id(), id -> nextEntityId.getAndIncrement());
    }

    public void unregisterAll() {
        npcUuids.clear();
        npcEntityIds.clear();
        visibleTo.clear();
    }

    /**
     * Revisa la posición del jugador contra todos los NPCs registrados en su
     * mundo, y spawnea/despawnea según entre o salga del radio de visibilidad.
     */
    public void updateVisibility(Player player, Iterable<NpcDefinition> allNpcs) {

        java.util.Set<String> currentlyVisible = visibleTo.computeIfAbsent(player.getUniqueId(),
                k -> new java.util.HashSet<>());

        for (NpcDefinition npc : allNpcs) {

            if (!npc.world().equals(player.getWorld().getName())) {
                if (currentlyVisible.remove(npc.id())) {
                    despawn(player, npc);
                }
                continue;
            }

            double distanceSquared = distanceSquared(player, npc);
            boolean inRange = distanceSquared <= (VISIBILITY_RADIUS * VISIBILITY_RADIUS);
            boolean isVisible = currentlyVisible.contains(npc.id());

            if (inRange && !isVisible) {
                // Solo se anota como visible si de verdad se envió: si no, el
                // despawn posterior mandaría un destroy de algo inexistente.
                if (spawn(player, npc)) {
                    currentlyVisible.add(npc.id());
                }
            } else if (!inRange && isVisible) {
                despawn(player, npc);
                currentlyVisible.remove(npc.id());
            }
        }
    }

    public void despawnAllForEveryone() {

        for (Map.Entry<UUID, java.util.Set<String>> entry : visibleTo.entrySet()) {

            org.bukkit.entity.Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            for (String npcId : entry.getValue()) {
                Integer entityId = npcEntityIds.get(npcId);
                if (entityId != null) {
                    renderer.despawnFor(player, entityId);
                }
            }
        }

        visibleTo.clear();
    }

    public void removeViewer(Player player) {
        visibleTo.remove(player.getUniqueId());
    }

    private boolean spawn(Player player, NpcDefinition npc) {

        UUID npcUuid = npcUuids.get(npc.id());
        Integer entityId = npcEntityIds.get(npc.id());

        if (npcUuid == null || entityId == null) {
            return false;
        }

        return renderer.spawnFor(player, npc, npcUuid, entityId);
    }

    private void despawn(Player player, NpcDefinition npc) {
        Integer entityId = npcEntityIds.get(npc.id());
        if (entityId != null) {
            renderer.despawnFor(player, entityId);
        }
    }

    private double distanceSquared(Player player, NpcDefinition npc) {
        double dx = player.getLocation().getX() - npc.x();
        double dy = player.getLocation().getY() - npc.y();
        double dz = player.getLocation().getZ() - npc.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public java.util.Optional<Integer> getEntityId(String npcId) {
        return java.util.Optional.ofNullable(npcEntityIds.get(npcId));
    }

}