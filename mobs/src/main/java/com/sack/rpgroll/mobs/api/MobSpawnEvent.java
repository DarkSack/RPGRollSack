package com.sack.rpgroll.mobs.api;

import com.sack.rpgroll.mobs.core.MobDefinition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando RPGRoll-Mobs termina de crear un mob (definición ya aplicada). */
public class MobSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;

    public MobSpawnEvent(LivingEntity entity, MobDefinition definition) {
        this.entity = entity;
        this.definition = definition;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobDefinition getDefinition() {
        return definition;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
