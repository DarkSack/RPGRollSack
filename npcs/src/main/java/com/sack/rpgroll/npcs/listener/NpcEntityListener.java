package com.sack.rpgroll.npcs.listener;

import com.sack.rpgroll.npcs.core.NpcActionExecutor;
import com.sack.rpgroll.npcs.core.NpcManager;
import com.sack.rpgroll.npcs.core.NpcSpawnManager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Clics sobre los NPCs (derecho e izquierdo), su invulnerabilidad y su
 * aparición al cargarse el chunk donde viven.
 */
public class NpcEntityListener implements Listener {

    private static final long COOLDOWN_MILLIS = 300L;

    private final NpcManager npcManager;
    private final NpcSpawnManager spawnManager;
    private final NpcActionExecutor actionExecutor;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    public NpcEntityListener(NpcManager npcManager, NpcSpawnManager spawnManager, NpcActionExecutor actionExecutor) {
        this.npcManager = npcManager;
        this.spawnManager = spawnManager;
        this.actionExecutor = actionExecutor;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {

        if (!isNpc(event.getRightClicked())) {
            return;
        }

        event.setCancelled(true);

        // El evento llega una vez por mano; solo cuenta la principal.
        if (event.getHand() == EquipmentSlot.HAND) {
            trigger(event.getPlayer(), event.getRightClicked());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {

        if (!isNpc(event.getEntity())) {
            return;
        }

        event.setCancelled(true);

        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
            trigger(player, event.getEntity());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        spawnManager.onChunkLoad(event.getChunk(), npcManager.getAll());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastInteraction.remove(event.getPlayer().getUniqueId());
    }

    private boolean isNpc(Entity entity) {
        return spawnManager.npcIdOf(entity).isPresent();
    }

    private void trigger(Player player, Entity entity) {

        long now = System.currentTimeMillis();
        Long last = lastInteraction.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MILLIS) {
            return;
        }
        lastInteraction.put(player.getUniqueId(), now);

        spawnManager.npcIdOf(entity)
                .flatMap(npcManager::get)
                .ifPresent(npc -> actionExecutor.execute(player, npc));
    }

}
