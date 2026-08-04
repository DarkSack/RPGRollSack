package com.sack.rpgroll.guilds.guild.event;

/**
 * Entrada del calendario de guild — spec: "eventos organizados por la
 * guild (torneos, cacerías, expediciones, raids, eventos internos)" y
 * "calendario (eventos/reuniones/guerras/raids)". Se modela como una
 * agenda simple: nombre + hora + descripción, con una mazmorra opcional
 * vinculada (las expediciones/raids en sí las resuelve RPGRoll-Dungeons).
 */
public class GuildEvent {

    private final String id;
    private String name;
    private String type;
    private String description;
    private long scheduledAtMillis;
    private String linkedDungeonId;
    private boolean announced;

    public GuildEvent(String id, String name, String type, String description, long scheduledAtMillis) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.scheduledAtMillis = scheduledAtMillis;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String type() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long scheduledAtMillis() {
        return scheduledAtMillis;
    }

    public void setScheduledAtMillis(long scheduledAtMillis) {
        this.scheduledAtMillis = scheduledAtMillis;
    }

    public String linkedDungeonId() {
        return linkedDungeonId;
    }

    public void setLinkedDungeonId(String linkedDungeonId) {
        this.linkedDungeonId = linkedDungeonId;
    }

    public boolean announced() {
        return announced;
    }

    public void markAnnounced() {
        this.announced = true;
    }

    public boolean isPast() {
        return System.currentTimeMillis() > scheduledAtMillis;
    }

}
