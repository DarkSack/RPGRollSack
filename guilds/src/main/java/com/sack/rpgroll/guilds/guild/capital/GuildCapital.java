package com.sack.rpgroll.guilds.guild.capital;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base de la guild — spec: "base/spawn/bank/NPCs/decoraciones/teleports".
 * NPCs y decoraciones estructurales quedan fuera de esta pasada (requieren
 * un sistema de esquemas/estructuras que no existe todavía); los puntos
 * de teletransporte nombrados cubren el uso práctico de "decoración" como
 * punto de interés navegable dentro de la capital.
 */
public class GuildCapital {

    private GuildPoint spawn;
    private GuildPoint bank;
    private final Map<String, GuildPoint> teleportPoints = new LinkedHashMap<>();

    public GuildPoint spawn() {
        return spawn;
    }

    public void setSpawn(GuildPoint spawn) {
        this.spawn = spawn;
    }

    public GuildPoint bank() {
        return bank;
    }

    public void setBank(GuildPoint bank) {
        this.bank = bank;
    }

    public Map<String, GuildPoint> teleportPoints() {
        return Map.copyOf(teleportPoints);
    }

    public void setTeleportPoint(String name, GuildPoint point) {
        teleportPoints.put(name.toLowerCase(java.util.Locale.ROOT), point);
    }

    public void removeTeleportPoint(String name) {
        teleportPoints.remove(name.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isEstablished() {
        return spawn != null;
    }

}
