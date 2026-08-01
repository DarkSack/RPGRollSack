package com.sack.rpgroll.npcs.render;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;

import com.sack.rpgroll.npcs.core.NpcDefinition;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.EnumSet;

/**
 * Renderiza NPCs como jugadores falsos vía packets de ProtocolLib.
 * <p>
 * El flujo por jugador que "ve" un NPC es:
 * 1. PLAYER_INFO (ADD_PLAYER) — registra el perfil falso en la tablist del
 * cliente,
 * requerido para que el cliente pueda renderizar la skin correctamente.
 * 2. NAMED_ENTITY_SPAWN — crea la entidad visible en el mundo, en la posición
 * del NPC.
 * 3. (Opcional, tras un delay) PLAYER_INFO (REMOVE_PLAYER) — saca al NPC de la
 * tablist visual sin eliminar la entidad, para que no ensucie la lista de
 * jugadores conectados mientras se sigue viendo en el mundo.
 * <p>
 * Cada NPC usa un UUID aleatorio fijo (generado una vez al crearse, no en cada
 * spawn) y un entityId único de int, gestionado por NpcEntityIdRegistry.
 */
public class FakePlayerRenderer {

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private static final int POSE_METADATA_INDEX = 6;

    public FakePlayerRenderer(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void spawnFor(Player viewer, NpcDefinition npc, UUID npcUuid, int entityId) {

        WrappedGameProfile profile = new WrappedGameProfile(npcUuid, npc.displayName());

        if (npc.hasCustomSkin()) {
            profile.getProperties().put(
                    "textures",
                    new WrappedSignedProperty(
                            "textures",
                            npc.skinValue(),
                            npc.skinSignature()));
        }

        sendPlayerInfoAdd(viewer, profile);
        sendNamedEntitySpawn(viewer, npc, npcUuid, entityId);
        applyPose(viewer, npc, entityId);

        // Remover de la tablist tras un breve delay, dejando la entidad visible
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendPlayerInfoRemove(viewer, npcUuid), 40L);
    }

    public void despawnFor(Player viewer, int entityId) {
        sendEntityDestroy(viewer, entityId);
    }

    private void sendPlayerInfoAdd(Player viewer, WrappedGameProfile profile) {

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

        packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));

        PlayerInfoData data = new PlayerInfoData(
                profile,
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(profile.getName()));

        packet.getPlayerInfoDataLists().write(1, List.of(data));

        sendPacket(viewer, packet);
    }

    private void sendPlayerInfoRemove(Player viewer, UUID npcUuid) {

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, List.of(npcUuid));

        sendPacket(viewer, packet);
    }

    private void sendNamedEntitySpawn(Player viewer, NpcDefinition npc, UUID npcUuid, int entityId) {

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, npcUuid);

        packet.getEntityTypeModifier().write(0, EntityType.PLAYER);

        packet.getDoubles()
                .write(0, npc.x())
                .write(1, npc.y())
                .write(2, npc.z());

        packet.getBytes()
                .write(0, (byte) (npc.pitch() * 256 / 360))
                .write(1, (byte) (npc.yaw() * 256 / 360));

        sendPacket(viewer, packet);
    }

    private void applyPose(Player viewer, NpcDefinition npc, int entityId) {

        Pose pose;

        try {
            pose = Pose.valueOf(npc.pose());
        } catch (IllegalArgumentException e) {
            pose = Pose.STANDING;
        }

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);

        WrappedDataWatcher.Serializer poseSerializer = WrappedDataWatcher.Registry
                .get(com.comphenix.protocol.wrappers.EnumWrappers.getEntityPoseClass());

        WrappedDataValue dataValue = new WrappedDataValue(
                POSE_METADATA_INDEX,
                poseSerializer,
                com.comphenix.protocol.wrappers.EnumWrappers.getEntityPoseConverter().getGeneric(toNmsPose(pose)));

        packet.getDataValueCollectionModifier().write(0, List.of(dataValue));

        sendPacket(viewer, packet);
    }

    private com.comphenix.protocol.wrappers.EnumWrappers.EntityPose toNmsPose(Pose bukkitPose) {

        return switch (bukkitPose) {
            case STANDING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.STANDING;
            case SLEEPING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.SLEEPING;
            case SWIMMING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.SWIMMING;
            case SNEAKING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.CROUCHING;
            case DYING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.DYING;
            case SITTING -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.SITTING;
            default -> com.comphenix.protocol.wrappers.EnumWrappers.EntityPose.STANDING;
        };

    }

    private void sendEntityDestroy(Player viewer, int entityId) {

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, List.of(entityId));

        sendPacket(viewer, packet);
    }

    private void sendPacket(Player viewer, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("✘ Error enviando packet de NPC: " + e.getMessage());
        }
    }

}