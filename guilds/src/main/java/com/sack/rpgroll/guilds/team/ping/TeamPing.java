package com.sack.rpgroll.guilds.team.ping;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.UUID;

/** Marca activa. Si {@code entity} no es null y sigue viva, sigue a esa entidad; si no, queda fija en {@code location}. */
public class TeamPing {

    private final UUID id = UUID.randomUUID();
    private final UUID teamId;
    private final UUID creatorId;
    private final PingType type;
    private final String label;
    private final Location location;
    private final Entity entity;
    private final long expiresAtMillis;

    public TeamPing(UUID teamId, UUID creatorId, PingType type, String label, Location location, Entity entity,
            long durationMillis) {
        this.teamId = teamId;
        this.creatorId = creatorId;
        this.type = type;
        this.label = label;
        this.location = location;
        this.entity = entity;
        this.expiresAtMillis = System.currentTimeMillis() + durationMillis;
    }

    public UUID id() {
        return id;
    }

    public UUID teamId() {
        return teamId;
    }

    public UUID creatorId() {
        return creatorId;
    }

    public PingType type() {
        return type;
    }

    public String label() {
        return label;
    }

    public boolean expired() {
        if (System.currentTimeMillis() > expiresAtMillis) {
            return true;
        }
        return entity != null && entity.isDead();
    }

    /** @return la ubicación actual de la marca — la de la entidad seguida si sigue viva, o la fija. */
    public Location currentLocation() {
        return entity != null && !entity.isDead() ? entity.getLocation() : location;
    }

}
