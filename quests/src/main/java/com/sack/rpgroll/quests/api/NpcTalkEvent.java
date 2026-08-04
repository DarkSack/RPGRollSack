package com.sack.rpgroll.quests.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara cuando un jugador habla con un NPC identificado por
 * {@code npcId}. RPGRoll-Quests escucha este evento para avanzar objetivos
 * TALK_TO_NPC/DELIVER_ITEM — cualquier sistema de NPCs (RPGRoll-NPCs u
 * otro) puede dispararlo sin que este addon dependa de esa implementación
 * concreta.
 */
public class NpcTalkEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String npcId;

    public NpcTalkEvent(Player player, String npcId) {
        this.player = player;
        this.npcId = npcId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getNpcId() {
        return npcId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
