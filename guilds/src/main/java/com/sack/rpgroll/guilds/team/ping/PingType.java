package com.sack.rpgroll.guilds.team.ping;

import org.bukkit.Particle;
import net.kyori.adventure.text.format.NamedTextColor;

/** Tipos de marca del sistema de pings (spec: "marcar enemigos, objetivos, loot, NPCs, lugares"). */
public enum PingType {

    ENEMY(Particle.ANGRY_VILLAGER, NamedTextColor.RED, "Enemigo"),
    OBJECTIVE(Particle.END_ROD, NamedTextColor.GOLD, "Objetivo"),
    LOOT(Particle.HAPPY_VILLAGER, NamedTextColor.YELLOW, "Botín"),
    NPC(Particle.WITCH, NamedTextColor.LIGHT_PURPLE, "NPC"),
    PLACE(Particle.END_ROD, NamedTextColor.AQUA, "Lugar");

    private final Particle particle;
    private final NamedTextColor color;
    private final String displayName;

    PingType(Particle particle, NamedTextColor color, String displayName) {
        this.particle = particle;
        this.color = color;
        this.displayName = displayName;
    }

    public Particle particle() {
        return particle;
    }

    public NamedTextColor color() {
        return color;
    }

    public String displayName() {
        return displayName;
    }

}
