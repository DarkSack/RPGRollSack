package com.sack.rpgroll.npcs.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import com.sack.rpgroll.npcs.core.NpcActionExecutor;
import com.sack.rpgroll.npcs.core.NpcDefinition;
import com.sack.rpgroll.npcs.core.NpcManager;
import com.sack.rpgroll.npcs.core.NpcSpawnManager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcInteractListener extends PacketAdapter {

    private static final long COOLDOWN_MILLIS = 300L;

    private final NpcManager npcManager;
    private final NpcSpawnManager spawnManager;
    private final NpcActionExecutor actionExecutor;
    private final Map<UUID, Long> lastInteraction = new HashMap<>();

    public NpcInteractListener(Plugin plugin, NpcManager npcManager, NpcSpawnManager spawnManager,
            NpcActionExecutor actionExecutor) {
        super(plugin, PacketType.Play.Client.USE_ENTITY);
        this.npcManager = npcManager;
        this.spawnManager = spawnManager;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {

        int clickedEntityId = event.getPacket().getIntegers().read(0);

        NpcDefinition npc = findNpcByEntityId(clickedEntityId);

        if (npc == null) {
            return;
        }

        Player player = event.getPlayer();

        if (isOnCooldown(player)) {
            return;
        }

        lastInteraction.put(player.getUniqueId(), System.currentTimeMillis());

        getPlugin().getServer().getScheduler().runTask(getPlugin(),
                () -> actionExecutor.execute(player, npc));
    }

    private boolean isOnCooldown(Player player) {
        Long last = lastInteraction.get(player.getUniqueId());
        return last != null && (System.currentTimeMillis() - last) < COOLDOWN_MILLIS;
    }

    private NpcDefinition findNpcByEntityId(int entityId) {

        for (NpcDefinition npc : npcManager.getAll()) {
            if (spawnManager.getEntityId(npc.id()).map(id -> id == entityId).orElse(false)) {
                return npc;
            }
        }

        return null;
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

}