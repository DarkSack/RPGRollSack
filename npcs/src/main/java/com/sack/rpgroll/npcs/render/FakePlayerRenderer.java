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

    /**
     * Se enciende cuando ProtocolLib no puede armar los packets contra este
     * servidor. Los NPCs se reintentaban en cada bloque caminado, así que un
     * único fallo llenaba la consola con cientos de stacktraces por minuto.
     */
    private boolean renderingUnavailable;

    public FakePlayerRenderer(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * @return true si el NPC quedó enviado al cliente. false significa que
     *         este servidor no puede renderizar NPCs y no vale la pena
     *         reintentar.
     */
    public boolean spawnFor(Player viewer, NpcDefinition npc, UUID npcUuid, int entityId) {

        if (renderingUnavailable) {
            return false;
        }

        try {
            doSpawnFor(viewer, npc, npcUuid, entityId);
            return true;

        } catch (RuntimeException e) {
            // ProtocolLib tira FieldAccessException/IllegalArgumentException
            // cuando la estructura del packet no coincide con la del servidor.
            reportUnavailable(e);
            return false;
        }
    }

    /**
     * Deja constancia una sola vez y apaga el renderizado.
     * <p>
     * El caso visto en producción: {@code Field index 0 is out of bounds for
     * length 0} al escribir el tipo de entidad en SPAWN_ENTITY — ProtocolLib
     * no encontró ese campo porque su versión no conoce el formato de packets
     * de ese servidor.
     */
    private void reportUnavailable(RuntimeException cause) {

        renderingUnavailable = true;

        plugin.getLogger().severe("✘ No se pueden renderizar NPCs en este servidor: "
                + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        plugin.getLogger().severe("  Casi siempre es ProtocolLib desactualizado para la versión de "
                + Bukkit.getServer().getMinecraftVersion() + " que corre este servidor.");
        plugin.getLogger().severe("  Actualiza ProtocolLib y reinicia. El resto de RPGRoll-NPCs sigue "
                + "funcionando; solo no se dibujan los NPCs.");
    }

    private void doSpawnFor(Player viewer, NpcDefinition npc, UUID npcUuid, int entityId) {

        WrappedGameProfile profile = new WrappedGameProfile(npcUuid, sanitizeProfileName(npc.id()));

        if (npc.hasCustomSkin()) {
            profile.getProperties().put(
                    "textures",
                    new WrappedSignedProperty(
                            "textures",
                            npc.skinValue(),
                            npc.skinSignature()));
        }

        sendPlayerInfoAdd(viewer, profile, npcUuid, npc.displayName());
        sendNamedEntitySpawn(viewer, npc, npcUuid, entityId);
        applyPose(viewer, npc, entityId);

        // Remover de la tablist tras un breve delay, dejando la entidad visible
        // Corre fuera del try de spawnFor, así que lleva su propia guarda.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (renderingUnavailable || !viewer.isOnline()) {
                return;
            }
            try {
                sendPlayerInfoRemove(viewer, npcUuid);
            } catch (RuntimeException e) {
                reportUnavailable(e);
            }
        }, 40L);
    }

    public void despawnFor(Player viewer, int entityId) {

        if (renderingUnavailable) {
            return;
        }

        try {
            sendEntityDestroy(viewer, entityId);
        } catch (RuntimeException e) {
            reportUnavailable(e);
        }
    }

    private void sendPlayerInfoAdd(Player viewer, WrappedGameProfile profile, UUID npcUuid, String displayName) {

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

        packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));

        // El overload de 4 args (profile, latency, gameMode, displayName) resuelve el UUID internamente
        // llamando a profile.getUUID(), que en este build de Paper tira "Unsupported getId() method" (la
        // reflexión de ProtocolLib para leer el id del GameProfile de Mojang no encuentra el accessor
        // esperado). Este overload recibe el UUID directo — ya lo tenemos como npcUuid — y lo evita.
        PlayerInfoData data = new PlayerInfoData(
                npcUuid,
                0,
                true,
                EnumWrappers.NativeGameMode.SURVIVAL,
                profile,
                WrappedChatComponent.fromText(displayName));

        packet.getPlayerInfoDataLists().write(1, List.of(data));

        sendPacket(viewer, packet);
    }

    /**
     * El "name" de un {@link WrappedGameProfile} viaja por el protocolo como
     * un username real de Minecraft (máx. 16 caracteres ASCII) — a diferencia
     * del nombre visible en la tablist, que se manda aparte como chat
     * component y no tiene ese límite. Acá solo se usa como identificador
     * interno del perfil falso, nunca se muestra al jugador.
     */
    private String sanitizeProfileName(String id) {

        String sanitized = id.replaceAll("[^a-zA-Z0-9_]", "");

        if (sanitized.isBlank()) {
            sanitized = "npc";
        }

        return sanitized.length() > 16 ? sanitized.substring(0, 16) : sanitized;
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

        // Si ProtocolLib no reconoce el campo de tipo de entidad, escribir en
        // el índice 0 revienta con "out of bounds for length 0". Se comprueba
        // antes para dar un error que explique qué pasa.
        if (packet.getEntityTypeModifier().size() == 0) {
            throw new IllegalStateException(
                    "SPAWN_ENTITY no expone un campo de tipo de entidad — ProtocolLib no entiende "
                            + "el formato de packets de este servidor");
        }

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