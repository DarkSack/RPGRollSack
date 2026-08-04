package com.sack.rpgroll.mobs.registry;

import com.sack.rpgroll.mobs.core.MobDefinition;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Contexto disponible al ejecutar una {@link com.sack.rpgroll.mobs.core.MobAction}. */
public record MobActionContext(LivingEntity mob, MobDefinition definition, LivingEntity target) {

    public Player targetPlayer() {
        return target instanceof Player player ? player : null;
    }

}
