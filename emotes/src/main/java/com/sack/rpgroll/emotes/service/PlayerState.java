package com.sack.rpgroll.emotes.service;

import org.bukkit.entity.Player;

/** Estados del jugador en los que no se puede lanzar un emote. */
public record PlayerState(boolean gliding, boolean swimming, boolean riding, boolean sleeping, boolean dead) {

    public static PlayerState of(Player p) {
        return new PlayerState(p.isGliding(), p.isSwimming(), p.isInsideVehicle(), p.isSleeping(), p.isDead());
    }

    /** true si cualquiera de los estados impide el emote. */
    public boolean busy() {
        return gliding || swimming || riding || sleeping || dead;
    }
}
