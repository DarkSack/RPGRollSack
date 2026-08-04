package com.sack.rpgroll.guilds.team;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Grupo temporal — para dungeons, jefes, quests, eventos o exploración/PvP.
 * Vive solo en memoria (no persiste entre reinicios: por diseño, un equipo
 * es una agrupación de sesión, no una organización permanente como
 * {@link com.sack.rpgroll.guilds.guild.Guild}).
 */
public class Team {

    private final UUID id = UUID.randomUUID();
    private final long createdAtMillis = System.currentTimeMillis();

    private UUID leaderId;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UUID, TeamRole> roles = new LinkedHashMap<>();

    private String name;
    private NamedTextColor color = NamedTextColor.AQUA;
    private Material icon = Material.WHITE_BANNER;
    private int maxPlayers = 6;
    private int minLevel = 0;

    private boolean shareXp = true;
    private boolean shareLoot = false;
    private boolean shareGold = false;

    private final Set<TeamBuff> enabledBuffs = EnumSet.noneOf(TeamBuff.class);
    private final Map<String, TeamWaypoint> waypoints = new LinkedHashMap<>();

    private String linkedDungeonId;
    private String linkedEventId;

    public Team(UUID leaderId) {
        this.leaderId = leaderId;
        this.members.add(leaderId);
        this.roles.put(leaderId, TeamRole.LEADER);
        this.name = "Equipo";
    }

    public UUID id() {
        return id;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public UUID leaderId() {
        return leaderId;
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public Set<UUID> members() {
        return Set.copyOf(members);
    }

    public int size() {
        return members.size();
    }

    public boolean contains(UUID playerId) {
        return members.contains(playerId);
    }

    public TeamRole roleOf(UUID playerId) {
        return roles.getOrDefault(playerId, TeamRole.GUEST);
    }

    public void addMember(UUID playerId, TeamRole role) {
        members.add(playerId);
        roles.put(playerId, role);
    }

    /** @return true si el equipo quedó vacío (había un solo miembro y era este). */
    public boolean removeMember(UUID playerId) {

        members.remove(playerId);
        roles.remove(playerId);

        if (members.isEmpty()) {
            return true;
        }

        if (leaderId.equals(playerId)) {
            leaderId = members.iterator().next();
            roles.put(leaderId, TeamRole.LEADER);
        }

        return false;
    }

    public void setRole(UUID playerId, TeamRole role) {
        if (members.contains(playerId) && !isLeader(playerId)) {
            roles.put(playerId, role);
        }
    }

    // ============ Configuración ============

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NamedTextColor color() {
        return color;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
    }

    public Material icon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(1, maxPlayers);
    }

    public int minLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = Math.max(0, minLevel);
    }

    public boolean shareXp() {
        return shareXp;
    }

    public void setShareXp(boolean shareXp) {
        this.shareXp = shareXp;
    }

    public boolean shareLoot() {
        return shareLoot;
    }

    public void setShareLoot(boolean shareLoot) {
        this.shareLoot = shareLoot;
    }

    public boolean shareGold() {
        return shareGold;
    }

    public void setShareGold(boolean shareGold) {
        this.shareGold = shareGold;
    }

    public Set<TeamBuff> enabledBuffs() {
        return EnumSet.copyOf(enabledBuffs.isEmpty() ? EnumSet.noneOf(TeamBuff.class) : enabledBuffs);
    }

    public boolean hasBuff(TeamBuff buff) {
        return enabledBuffs.contains(buff);
    }

    public void toggleBuff(TeamBuff buff) {
        if (!enabledBuffs.remove(buff)) {
            enabledBuffs.add(buff);
        }
    }

    public Map<String, TeamWaypoint> waypoints() {
        return Map.copyOf(waypoints);
    }

    public void addWaypoint(TeamWaypoint waypoint) {
        waypoints.put(waypoint.name().toLowerCase(java.util.Locale.ROOT), waypoint);
    }

    public void removeWaypoint(String name) {
        waypoints.remove(name.toLowerCase(java.util.Locale.ROOT));
    }

    public String linkedDungeonId() {
        return linkedDungeonId;
    }

    public void setLinkedDungeonId(String linkedDungeonId) {
        this.linkedDungeonId = linkedDungeonId;
    }

    public String linkedEventId() {
        return linkedEventId;
    }

    public void setLinkedEventId(String linkedEventId) {
        this.linkedEventId = linkedEventId;
    }

}
