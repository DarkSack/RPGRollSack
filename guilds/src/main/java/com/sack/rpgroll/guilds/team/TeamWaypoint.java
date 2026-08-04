package com.sack.rpgroll.guilds.team;

/** Punto de referencia compartido por el equipo (spec: "sistema de waypoints integrado"). */
public record TeamWaypoint(String name, String world, double x, double y, double z, long createdAtMillis) {
}
