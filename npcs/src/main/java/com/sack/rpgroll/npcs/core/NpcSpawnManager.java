package com.sack.rpgroll.npcs.core;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.sack.rpgroll.util.ComponentUtils;

import io.papermc.paper.datacomponent.item.ResolvableProfile;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Pose;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pone cada NPC en el mundo como un {@link Mannequin}: la entidad con forma
 * de jugador que trae el propio servidor desde 1.21.9.
 * <p>
 * Antes se dibujaban jugadores falsos a base de packets de ProtocolLib. Eso se
 * rompía con cada versión nueva del protocolo (en 26.x el packet de la lista
 * de jugadores ya no tiene el campo que se escribía) y además el nombre sobre
 * la cabeza era el del perfil falso, o sea el id interno del NPC, sin colores.
 * El Mannequin es una entidad normal: lleva nombre con formato, skin, pose y
 * rotación, lo ven todos los jugadores (también los de Bedrock) y los clics
 * llegan como eventos de Bukkit.
 * <p>
 * Las entidades NO son persistentes: no se guardan con el chunk. Se crean al
 * cargar el plugin o el chunk y desaparecen al descargarse, así el YAML es la
 * única fuente de verdad y no quedan duplicados tras un cierre brusco.
 */
public class NpcSpawnManager {

    private final Plugin plugin;
    private final NamespacedKey npcIdKey;

    /** id del NPC -> UUID de su Mannequin vivo. */
    private final Map<String, UUID> spawned = new HashMap<>();

    public NpcSpawnManager(Plugin plugin) {
        this.plugin = plugin;
        this.npcIdKey = new NamespacedKey(plugin, "npc_id");
    }

    /** Quita todos los NPCs del mundo y vuelve a crear los definidos. */
    public void respawnAll(Collection<NpcDefinition> npcs) {
        despawnAll();
        npcs.forEach(this::spawnIfLoaded);
    }

    /** Crea el NPC si su chunk está cargado; si no, lo hará {@link #onChunkLoad}. */
    public void spawnIfLoaded(NpcDefinition npc) {

        World world = Bukkit.getWorld(npc.world());
        if (world == null) {
            plugin.getLogger().warning("✘ NPC '" + npc.id() + "': el mundo '" + npc.world() + "' no está cargado.");
            return;
        }

        if (!world.isChunkLoaded(chunkX(npc), chunkZ(npc)) || isAlive(npc.id())) {
            return;
        }

        Location location = new Location(world, npc.x(), npc.y(), npc.z(), npc.yaw(), npc.pitch());
        Mannequin mannequin = world.spawn(location, Mannequin.class, entity -> configure(entity, npc));

        if (!mannequin.isValid()) {
            plugin.getLogger().warning("✘ NPC '" + npc.id() + "': otro plugin canceló su aparición en "
                    + npc.world() + " aun a prioridad HIGHEST. Revisa protecciones de regiones o anti-mobs.");
            return;
        }

        spawned.put(npc.id(), mannequin.getUniqueId());
    }

    /** Llamado al cargarse un chunk: crea los NPCs que caen dentro. */
    public void onChunkLoad(Chunk chunk, Collection<NpcDefinition> npcs) {

        String worldName = chunk.getWorld().getName();

        for (NpcDefinition npc : npcs) {
            if (npc.world().equals(worldName) && chunkX(npc) == chunk.getX() && chunkZ(npc) == chunk.getZ()) {
                spawnIfLoaded(npc);
            }
        }
    }

    public void despawnAll() {

        for (UUID uuid : spawned.values()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }

        spawned.clear();
    }

    /** @return el id del NPC si la entidad es uno de los nuestros. */
    public Optional<String> npcIdOf(Entity entity) {
        return Optional.ofNullable(
                entity.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING));
    }

    private boolean isAlive(String npcId) {
        UUID uuid = spawned.get(npcId);
        if (uuid == null) {
            return false;
        }
        Entity entity = Bukkit.getEntity(uuid);
        return entity != null && entity.isValid();
    }

    private static int chunkX(NpcDefinition npc) {
        return (int) Math.floor(npc.x()) >> 4;
    }

    private static int chunkZ(NpcDefinition npc) {
        return (int) Math.floor(npc.z()) >> 4;
    }

    private void configure(Mannequin entity, NpcDefinition npc) {

        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, npc.id());

        entity.customName(ComponentUtils.parse(npc.displayName()));
        entity.setCustomNameVisible(true);
        // Sin esto el Mannequin enseña "NPC" debajo del nombre.
        entity.setDescription(null);

        entity.setImmovable(true);
        entity.setAI(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setCollidable(false);
        entity.setRemoveWhenFarAway(false);

        entity.setRotation(npc.yaw(), npc.pitch());
        entity.setBodyYaw(npc.yaw());

        if (npc.hasCustomSkin()) {
            entity.setProfile(ResolvableProfile.resolvableProfile()
                    .addProperty(new ProfileProperty("textures", npc.skinValue(), npc.skinSignature()))
                    .build());
        }

        Pose pose = poseOf(npc.pose());
        if (pose != Pose.STANDING) {
            entity.setPose(pose, true);
        }
    }

    /** La pose pedida si el Mannequin la admite; si no, de pie. */
    public static Pose poseOf(String name) {
        try {
            Pose pose = Pose.valueOf(name);
            return Mannequin.validPoses().contains(pose) ? pose : Pose.STANDING;
        } catch (IllegalArgumentException | NullPointerException e) {
            return Pose.STANDING;
        }
    }

}
