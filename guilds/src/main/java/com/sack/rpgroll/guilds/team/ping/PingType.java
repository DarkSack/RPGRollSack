package com.sack.rpgroll.guilds.team.ping;

import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Particle;
import net.kyori.adventure.text.format.NamedTextColor;

/** Tipos de marca del sistema de pings (spec: "marcar enemigos, objetivos, loot, NPCs, lugares"). */
public enum PingType {

    ENEMY(Particle.ANGRY_VILLAGER, NamedTextColor.RED),
    OBJECTIVE(Particle.END_ROD, NamedTextColor.GOLD),
    LOOT(Particle.HAPPY_VILLAGER, NamedTextColor.YELLOW),
    NPC(Particle.WITCH, NamedTextColor.LIGHT_PURPLE),
    PLACE(Particle.END_ROD, NamedTextColor.AQUA);

    private final Particle particle;
    private final NamedTextColor color;

    PingType(Particle particle, NamedTextColor color) {
        this.particle = particle;
        this.color = color;
    }

    public Particle particle() {
        return particle;
    }

    public NamedTextColor color() {
        return color;
    }

    public String displayName(LangManager lang) {
        return lang.raw("team.pingtype." + name().toLowerCase(java.util.Locale.ROOT));
    }

}
