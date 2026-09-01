package com.sack.rpgroll.fx.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Quién disparó un efecto y, opcionalmente, su objetivo — todo lo que un
 * {@link com.sack.rpgroll.fx.core.EffectStep} necesita para resolver
 * "self"/"target"/"all_nearby".
 */
public record EffectContext(Player caster, Entity targetEntity, Location targetLocation) {

    public static EffectContext of(Player caster) {
        return new EffectContext(caster, null, null);
    }

    public static EffectContext of(Player caster, Entity target) {
        return new EffectContext(caster, target, target != null ? target.getLocation() : null);
    }

    public static EffectContext of(Player caster, Location targetLocation) {
        return new EffectContext(caster, null, targetLocation);
    }

    /** La ubicación del objetivo, sea de una entidad o un punto fijo — null si no hay ninguno. */
    public Location resolvedTargetLocation() {
        if (targetEntity != null) {
            return targetEntity.getLocation();
        }
        return targetLocation;
    }

}
