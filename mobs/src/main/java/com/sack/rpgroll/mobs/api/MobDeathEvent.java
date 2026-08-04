package com.sack.rpgroll.mobs.api;

import com.sack.rpgroll.mobs.core.MobDefinition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara cuando un mob de RPGRoll-Mobs muere — más específico que el
 * {@code EntityDeathEvent} vanilla, ya que identifica la DEFINICIÓN
 * exacta (dos mobs pueden compartir el mismo EntityType base). Otros
 * addons (ej. RPGRoll-Quests con un objetivo custom) pueden escuchar esto.
 */
public class MobDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobDefinition definition;
    private final Player killer;

    public MobDeathEvent(LivingEntity entity, MobDefinition definition, Player killer) {
        this.entity = entity;
        this.definition = definition;
        this.killer = killer;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobDefinition getDefinition() {
        return definition;
    }

    public Player getKiller() {
        return killer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
